# Spring Batch 재시도(Retry) / 스킵(Skip) 동작 정리

> 이 문서는 이 프로젝트가 실제로 사용하는 Spring Batch `6.0.2`의 소스코드
> (`ChunkOrientedStep`, `ChunkOrientedStepBuilder`, `org.springframework.core.retry.*`)를
> 직접 확인해서 정리한 내용입니다.

## 1. 청크 처리와 트랜잭션 경계 (기본 구조)

`ChunkOrientedStep.doExecute()`는 다음과 같이 동작합니다.

```java
while (moreItems() && !interrupted()) {
    transactionTemplate.executeWithoutResult(status -> {
        processNextChunk(...);              // read N개 -> process 각각 -> write 전체
        compositeItemStream.update(...);    // reader/writer의 restart 상태 저장
        jobRepository.updateExecutionContext(...);
        jobRepository.update(stepExecution);
    });
}
```

핵심은 **"청크 하나 read+process+write" 전체와 "그 청크의 ExecutionContext/StepExecution
갱신"이 전부 하나의 트랜잭션**이라는 점입니다. 청크 중간에 실패하면 이 트랜잭션 전체가
롤백되므로, 이번 청크에서 처리한 비즈니스 데이터뿐 아니라 **"어디까지 읽었다"는 재시작
상태 자체도 저장되지 않습니다.**

## 2. `faultTolerant()` = true/false 차이

`ChunkOrientedStep` 내부에는 read/process/write마다 이런 분기가 있습니다.

```java
private I doRead() throws Exception {
    if (this.faultTolerant) {
        return this.retryTemplate.execute(retryableRead);   // 재시도 엔진을 탐
    } else {
        return this.itemReader.read();                       // 바로 실행, 실패하면 즉시 예외 전파
    }
}
```

- **`faultTolerant = false` (기본값)**: read/process/write 중 하나라도 예외가 나면 그 즉시
  예외가 전파되고, 현재 청크 트랜잭션이 롤백되면서 Step이 실패합니다. `retry()`/`skip()`을
  설정해봤자 **`faultTolerant()`를 호출하지 않으면 전부 무시됩니다.** (실무에서 자주 빠뜨리는
  실수입니다.)
- **`faultTolerant = true`**: read/process/write가 `RetryTemplate`으로 감싸져서
  `retryPolicy`에 맞게 재시도되고, 재시도가 소진되면 `skipPolicy`로 스킵 가능 여부를
  판단합니다. `.faultTolerant()`를 호출해야만 이 경로를 탑니다.

```java
new StepBuilder(jobRepository)
    .<I, O>chunk(10)
    .reader(reader).processor(processor).writer(writer)
    .faultTolerant()   // 반드시 호출 — 이게 없으면 아래 retry/skip 설정은 전부 무의미
    .retry(DeadlockLoserDataAccessException.class)
    .retryLimit(3)
    .skip(FlatFileParseException.class)
    .skipLimit(10)
    .build();
```

## 3. Retry는 정확히 어디서, 어떤 단위로 동작하는가

`faultTolerant=true`일 때 read/process/write 각각을 `Retryable`로 감싸서
`RetryTemplate.execute()`에 넘깁니다. (참고: 스프링 코어의 새 `org.springframework.core.retry`
API이며, 예전 `spring-retry` 프로젝트의 `RetryTemplate`과는 다른 클래스입니다.)

- **read / process**: 아이템 **1개 단위**로 재시도합니다. `itemReader.read()` 한 번,
  `itemProcessor.process(item)` 한 번이 재시도 대상입니다.
- **write**: **청크 전체**(`itemWriter.write(chunk)`)가 재시도 단위입니다. writer는 보통
  배치 insert/update처럼 청크 전체를 한 번에 처리하기 때문에, 실패했을 때 "그 청크 안의
  어느 아이템"이 문제인지 알 수 없기 때문입니다.

재시도가 소진됐을 때(`RetryException` 발생) 동작이 read/process와 write에서 다릅니다.

- **read/process**: `skipPolicy.shouldSkip(cause, ...)`로 스킵 가능한지 확인 → 가능하면
  그 아이템만 건너뛰고 계속, 불가능하면 `NonSkippableXxxException`을 던져
  **청크 전체 롤백**.
- **write**: 청크 전체 재시도가 소진되면, 스킵 가능한 예외인 경우 **"스캔(scan)" 모드**로
  전환합니다. 청크를 1건씩 잘라서 `itemWriter.write(singleItemChunk)`로 개별
  재실행합니다 (`ChunkOrientedStep.scan()`). 이 과정에서 실패하는 그 아이템만 스킵되고,
  나머지는 정상 커밋됩니다.

즉 **"청크 단위 재시도"는 사실상 write 단계에만 해당하는 이야기**이고, read/process는
원래도 아이템 단위입니다.

## 4. Skip vs Retry 비교

| | Retry | Skip |
|---|---|---|
| 의미 | 같은 작업을 다시 시도 (같은 아이템, 같은 트랜잭션 안에서) | 그 아이템을 포기하고 넘어감 |
| 쓰는 상황 | 일시적 오류 (deadlock, 네트워크 순단, 낙관적 락 충돌 등) — 다시 하면 될 수도 있는 것 | 영구적 오류 (데이터 자체가 잘못됨) — 몇 번을 다시 해도 안 되는 것 |
| 발생 순서 | 먼저 시도됨 | retry가 다 소진된 뒤에 판단됨 |
| 설정 메서드 | `.retry(Class...)`, `.retryLimit(n)`, `.retryPolicy(...)` | `.skip(Class...)`, `.skipLimit(n)`, `.skipPolicy(...)` |
| 한도 초과 시 | 그냥 재시도를 멈추고 skip 판단으로 넘어감 | `SkipLimitExceededException` → Step 실패 |
| 기본값 | 재시도 정책 없음(재시도 안 함) | `NeverSkipItemSkipPolicy` (스킵 안 함) |

## 5. Restart는 Retry와 다른 개념입니다

**네, 완전히 다른 메커니즘입니다.**

| | Retry | Restart |
|---|---|---|
| 범위 | 같은 Step 실행(같은 `StepExecution`), 같은 트랜잭션 안에서 자동으로 발생 | Job/Step이 **실패한 뒤**, 별도로 Job을 다시 실행해야 함 (`JobLauncher.run(...)` 재호출 또는 `JobOperator.restart(executionId)`) |
| 트리거 | read/process/write 중 예외 발생 시 즉시, 프레임워크가 자동으로 | 운영자/스케줄러가 명시적으로 재실행 |
| 새 실행 단위 생성 여부 | 생성 안 함 (같은 청크, 같은 시도 안에서 반복) | 같은 `JobInstance` 아래 **새 `StepExecution`**이 생성됨 |
| 재개 지점 | 실패한 그 read/process/write 호출 자체를 다시 호출 | **마지막으로 성공 커밋된 청크 경계**부터 (아래 참고) |

### Restart는 어느 지점부터 재개되는가

ExecutionContext(reader의 "몇 번째까지 읽었다" 같은 상태)는 **그 청크가 정상 커밋될 때만**
같이 저장됩니다(1번 항목 참고). 그래서:

- 청크 크기 10, 1~10번 청크 커밋 성공, 11~20번 청크 처리 중 15번 아이템에서 스킵 불가능한
  예외 발생 → 이 청크 트랜잭션 전체 롤백 → Job 실패
- **재시작하면 reader는 "10번까지 읽었다"는 상태에서 다시 시작** → 11번부터 다시 읽음
- 11~14번은 실패 전 이미 처리에 성공했었지만, 트랜잭션이 롤백됐으므로 **11~20번 전체가
  처음부터 다시 처리됩니다.**

즉 **재시작 단위는 "마지막으로 성공 커밋된 청크 경계"이지, "실패한 그 아이템"이 아닙니다.**
실패한 청크는 성공했던 부분까지 포함해서 통째로 재실행됩니다.

## 6. 예제 코드

### 6-1. Retry: 일시적 오류(락 경합)에 재시도 적용

```java
@Bean
public Step retryableStep(
    JobRepository jobRepository,
    PlatformTransactionManager transactionManager,
    ItemReader<Order> orderReader,
    ItemWriter<Order> orderWriter
) {
    return new StepBuilder(jobRepository)
        .<Order, Order>chunk(10)
        .transactionManager(transactionManager)
        .reader(orderReader)
        .writer(orderWriter)
        .faultTolerant()
        .retry(org.springframework.dao.DeadlockLoserDataAccessException.class)
        .retryLimit(3)               // 총 시도 = 1회 + retryLimit
        .build();
}
```

### 6-2. Skip: 파싱 실패한 레코드만 건너뛰기

```java
@Bean
public Step skippableStep(
    JobRepository jobRepository,
    PlatformTransactionManager transactionManager,
    FlatFileItemReader<Order> orderReader,
    ItemWriter<Order> orderWriter
) {
    return new StepBuilder(jobRepository)
        .<Order, Order>chunk(10)
        .transactionManager(transactionManager)
        .reader(orderReader)
        .writer(orderWriter)
        .faultTolerant()
        .skip(org.springframework.batch.infrastructure.item.file.FlatFileParseException.class)
        .skipLimit(20)                // 스킵 20건 넘으면 Step 실패
        .skipListener(new SkipListener<Order, Order>() {
            @Override
            public void onSkipInRead(Throwable t) {
                log.warn("읽기 스킵: {}", t.getMessage());
            }
        })
        .build();
}
```

### 6-3. Retry + Skip + 백오프(지수 백오프) 함께

```java
import org.springframework.core.retry.RetryPolicy;
import java.time.Duration;

@Bean
public Step faultTolerantStep(
    JobRepository jobRepository,
    PlatformTransactionManager transactionManager,
    ItemReader<Order> orderReader,
    ItemWriter<Order> orderWriter
) {
    RetryPolicy retryPolicy = RetryPolicy.builder()
        .maxRetries(3)
        .delay(Duration.ofMillis(500))   // 첫 재시도 전 대기 시간
        .multiplier(2.0)                 // 지수 백오프 배수
        .maxDelay(Duration.ofSeconds(5)) // 최대 대기 시간
        .includes(java.util.Set.of(org.springframework.dao.DeadlockLoserDataAccessException.class))
        .build();

    return new StepBuilder(jobRepository)
        .<Order, Order>chunk(10)
        .transactionManager(transactionManager)
        .reader(orderReader)
        .writer(orderWriter)
        .faultTolerant()
        .retryPolicy(retryPolicy)
        .skip(org.springframework.batch.infrastructure.item.file.FlatFileParseException.class)
        .skipLimit(20)
        .build();
}
```

## 7. 실무에서 고려해야 할 점

1. **멱등성(idempotency) 설계 필수**: 청크는 재시작 시 통째로 재실행될 수 있습니다. writer가
   순수 insert만 한다면 재시작 시 중복 insert 위험이 있으니, upsert나 unique 제약 +
   중복 예외 skip 처리를 고려해야 합니다.

2. **트랜잭션 밖의 부작용(side effect) 주의**: 청크 트랜잭션 롤백은 "그 트랜잭션에 참여한
   리소스"만 되돌립니다. writer/processor 안에서 이메일 발송, 외부 REST API 호출,
   파일 쓰기처럼 **트랜잭션이 걸리지 않는 부작용**을 실행하면, 청크가 롤백되고 재실행돼도
   그 부작용은 이미 일어난 채로 또 실행됩니다(예: 재시작 시 알림 메일이 두 번 감). 이런
   부작용은 별도로 멱등하게 만들거나 트랜잭션 커밋 이후로 미뤄야 합니다.

3. **retry 대상 예외는 "일시적" 오류로 좁게**: `.retry()` 없이 `.retryLimit()`만 쓰면
   기본값이 `Exception.class`(단, `Error`류는 제외)입니다. 데이터 자체가 잘못된 경우까지
   몇 번이고 재시도하다가 스킵 판정으로 넘어가느라 불필요하게 느려질 수 있으니, 락 경합,
   커넥션 순단처럼 "다시 하면 될 수도 있는" 예외로 한정하는 게 좋습니다.

4. **skip 대상은 "이 레코드만 포기해도 되는" 경우로 한정**: 파싱 실패, 검증 실패처럼 그
   레코드 하나가 이상한 경우로 좁혀야지, 광범위하게 잡으면 실제 심각한 버그도 조용히
   건너뛰고 넘어가는 사고가 납니다. `skipLimit`도 무제한이 아니라 명시적으로 낮게 잡아서,
   스킵이 너무 많이 발생하면 Job 자체를 실패시켜 사람이 알아채게 해야 합니다.

5. **백오프(backoff)**: `.retryLimit()`만 쓰면 고정 딜레이로 재시도합니다. 지수
   백오프/최대 지연이 필요하면 6-3 예제처럼 `RetryPolicy.builder()`로 직접 만들어
   `.retryPolicy(...)`에 넘겨야 합니다.

6. **모니터링**: `RetryListener`/`SkipListener`를 등록해서 재시도/스킵이 발생할 때마다
   로그·메트릭을 남겨두세요. 그렇지 않으면 운영 중 데이터가 조용히 스킵되고 있는데
   아무도 모르는 상황이 생길 수 있습니다.

7. **청크 크기와 write 재시도의 트레이드오프**: 청크가 클수록 커밋 오버헤드는 줄지만,
   write 재시도 소진 후 "스캔(1건씩 재시도)"이 발생하면 청크 크기만큼 개별 write가
   다시 나가므로 그만큼 느려집니다. 실패가 잦을 것으로 예상되는 스텝은 청크 크기를
   작게 가져가는 게 스캔 비용을 줄이는 데 유리합니다.

## 참고 소스 위치 (spring-batch-core 6.0.2)

- `org.springframework.batch.core.step.item.ChunkOrientedStep` — 청크 처리/재시도/스킵/스캔 로직 본체
- `org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder` — `faultTolerant()`/`retry()`/`skip()` 등 설정 API
- `org.springframework.core.retry.RetryPolicy`, `RetryTemplate` — 실제 재시도 엔진 (Spring Framework 7의 신규 코어 API)
