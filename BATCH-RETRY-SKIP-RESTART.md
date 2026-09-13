# Spring Batch Retry / Skip / Restart 동작 원리와 멱등성

> `spring-batch-core 6.0.2` 소스(`ChunkOrientedStep`, `ChunkOrientedStepBuilder`,
> `org.springframework.core.retry.RetryTemplate`)를 직접 확인해서 정리한 내용입니다.
> Retry/Skip 자체의 API 사용법은 `BATCH-RETRY-SKIP.md`를 참고하고, 이 문서는
> **"이 셋이 실제로 어떻게 다르게 동작하는가"**와 **"그로 인해 무엇을 조심해야 하는가"**에
> 집중합니다.

## 1. 세 가지는 서로 다른 층위의 메커니즘입니다

| | 발동 시점 | 범위 | 트랜잭션에 미치는 영향 |
|---|---|---|---|
| **Retry** | read/process/write 도중 예외 발생 즉시, 같은 청크 트랜잭션이 **진행 중인 도중** | 같은 호출(아이템 1개, 또는 write는 청크 전체)만 다시 실행 | **롤백 없음** — 트랜잭션은 계속 열려 있는 채로 같은 작업을 한 번 더 시도 |
| **Skip** | retry가 소진된 뒤, 그 예외가 스킵 가능하다고 판단될 때 | 그 아이템 하나만 포기 | 나머지 아이템은 정상적으로 같은 트랜잭션에서 계속 처리·커밋 |
| **Restart** | Step/Job이 **실패로 끝난 뒤**, Job을 다시 실행할 때 | 마지막으로 커밋된 청크 이후 전체 | 실패했던 청크는 이미 롤백된 상태 — 그 이후를 처음부터 다시 시작 |

세 개를 하나의 흐름으로 이어보면:

```
아이템 처리 실패
  │
  ▼
[Retry] 같은 작업을 retryLimit만큼 재시도 (트랜잭션 안 그대로, 롤백 없음)
  │
  ├─ 성공 → 청크 계속 진행
  │
  ▼ 그래도 실패
[Skip 판단]
  ├─ 스킵 가능 → 그 아이템만 포기, 청크는 계속 진행 → 청크 커밋
  │
  ▼ 스킵 불가능
청크 트랜잭션 전체 롤백 → Step 실패 → (분기 없으면) Job 실패
  │
  ▼ (운영자가 Job을 재실행하면)
[Restart] 마지막으로 커밋된 청크 다음부터, 실패했던 청크를 통째로 재실행
```

## 2. Retry가 "트랜잭션을 롤백하지 않는다"는 것의 실제 의미

`ChunkOrientedStep.writeChunk()`의 재시도 코드:

```java
private void doWrite(Chunk<O> chunk) throws Exception {
    if (this.faultTolerant) {
        Retryable<Void> retryableWrite = new Retryable<>() {
            public Void execute() throws Throwable {
                itemWriter.write(chunk);   // 재시도할 때마다 "같은 chunk"로 다시 호출
                return null;
            }
        };
        this.retryTemplate.execute(retryableWrite);
    }
}
```

`RetryTemplate.execute()`는 실패한 작업을 **그냥 다시 호출**할 뿐입니다. 실패 직전까지 그
트랜잭션 안에서 벌어진 일(예: 이미 실행된 일부 INSERT 문)을 되돌리는 어떤 동작도 하지
않습니다. 왜냐하면 애초에 "실패 = 롤백해야 할 대상이 생겼다"는 전제 자체가 성립하지
않을 수 있기 때문입니다 — retry는 **"아직 트랜잭션을 롤백할지 말지 결정하기 전에, 같은 걸
몇 번 더 찔러보는" 단계**이지, 복구(recovery) 단계가 아닙니다.

### 왜 이게 문제가 되는가 — 부분 실행 + 재시도

청크 10개를 배치 insert하는 writer가 있다고 합시다. 1~7번까지는 insert가 실행됐고
(트랜잭션 안에 커밋 안 된 채로 남아 있음), 8번에서 제약조건 위반으로 예외가 터졌습니다.

- retry가 걸리면 **1~7번을 포함한 전체 chunk로 `write()`가 다시 호출**됩니다.
- unique 제약이 있으면: 재시도에서 1~7번이 또 insert되려다 중복 키 에러 → 그나마 눈에
  보이는 실패.
- unique 제약이 없으면: **1~7번이 똑같이 한 번 더 insert된 채로, 이번엔 8번의 원인이
  해결돼서(혹은 skip 처리돼서) 트랜잭션이 그냥 커밋됨.** 에러도, 실패 로그도 없이 데이터만
  중복됩니다.

### Restart로 인한 중복과 무엇이 다른가

- **Restart발 중복**: Job이 **실패해야만** 일어납니다. 실패라는 뚜렷한 신호가 있고,
  운영자가 "재시작했으니 중복 체크가 필요하다"고 인지할 여지가 있습니다.
- **Retry발 중복**: Job이 **끝까지 성공한 것처럼 보이는 정상 실행 도중에, 조용히**
  일어날 수 있습니다. 실패 알림도, 재시작 이력도 없이 데이터만 두 번 들어갑니다. 원인을
  추적하기가 restart발 중복보다 훨씬 어렵습니다.

## 3. 실무에서 주의할 점

1. **writer는 upsert로 설계**: 순수 `INSERT` 대신 `INSERT ... ON DUPLICATE KEY UPDATE`
   (MySQL) / `MERGE`(H2, Oracle) 등으로, 같은 요청이 여러 번 들어와도 결과가 같도록
   만듭니다.

2. **자연키/unique 제약 + skip 조합**: 중복 삽입이 제약 위반으로 걸리도록 스키마를
   설계하고, 그 예외(예: `DuplicateKeyException`)를 skip 대상으로 등록해서 "이미 들어간
   건 조용히 넘어가기"로 처리합니다. 이렇게 하면 최소한 예외가 발생해 눈에 띄기라도
   합니다.

3. **process()/write() 안의 외부 부작용(side effect)도 동일한 문제**: 결제 API 호출,
   알림 발송처럼 **트랜잭션 밖에서 일어나는 부작용**을 이 안에서 실행한다면, 재시도 때
   그 호출이 또 나갈 수 있습니다. DB 트랜잭션 롤백으로는 절대 되돌릴 수 없는 부작용이므로,
   호출하는 대상 시스템이 지원한다면 idempotency key(요청마다 고유 ID를 실어 보내서
   외부 시스템이 중복 요청을 스스로 걸러내게 하는 방식)로 방어해야 합니다.

4. **retry 대상 예외는 좁게, "재시도하면 나아질 수 있는" 것으로 한정**: 락 경합, 커넥션
   순단처럼 일시적인 것만 retry 대상으로 등록하세요. 데이터 자체가 잘못된 경우(예:
   `DataIntegrityViolationException`)를 retry 대상으로 잡으면, 위에서 설명한 부분
   write + 재시도 시나리오를 그냥 반복해서 악화시킬 뿐입니다 — 애초에 그런 예외는 retry가
   아니라 skip 대상이어야 합니다.

5. **restart 시 재처리 범위도 같은 이유로 위험**: 실패했던 청크는 성공했던 부분까지
   포함해서 통째로 다시 처리되므로(관련 문서: `BATCH-RETRY-SKIP.md` 5번), writer가
   멱등하지 않으면 restart에서도 동일한 중복 문제가 발생합니다. 즉 **retry용으로 만든
   멱등한 writer는 자연스럽게 restart 안전성도 함께 얻게 됩니다** — 하나의 설계로 두
   가지 위험을 같이 방어하는 셈입니다.

6. **검증 방법**: 테스트에서 일부러 write 도중 예외를 던지도록 만들어서(예: writer를
   감싸서 첫 호출은 실패하게 함) retry가 실제로 어떻게 동작하는지, 중복이 발생하는지를
   직접 확인해보는 것을 권장합니다. "정상 케이스만 통과하는 테스트"로는 이 문제를 절대
   발견할 수 없습니다.

## 4. 예제: 멱등하지 않은 writer vs 멱등한 writer

### 위험한 형태 (순수 insert)

```java
@Bean
public JdbcBatchItemWriter<Order> orderWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<Order>()
        .dataSource(dataSource)
        .sql("INSERT INTO orders (id, status) VALUES (:id, :status)")
        .beanMapped()
        .build();
}
```
재시도 시 이미 insert된 앞부분 아이템이 또 insert 시도됨 — unique 제약이 없으면 중복 행,
있으면 재시도 자체가 실패로 끝남.

### 안전한 형태 (upsert)

```java
@Bean
public JdbcBatchItemWriter<Order> orderWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<Order>()
        .dataSource(dataSource)
        .sql("""
            MERGE INTO orders (id, status)
            KEY (id)
            VALUES (:id, :status)
            """)
        .beanMapped()
        .build();
}
```
같은 `chunk`로 몇 번을 재시도해도 결과가 같습니다 — 이미 들어간 행은 같은 값으로
덮어쓸 뿐, 중복 행이 생기지 않습니다.

## 참고

- 이 문서와 짝을 이루는 문서: `BATCH-RETRY-SKIP.md` (retry/skip API 사용법,
  faultTolerant true/false 차이, restart 재개 지점, 청크/트랜잭션 기본 구조)
- 참고 소스: `org.springframework.batch.core.step.item.ChunkOrientedStep`,
  `org.springframework.core.retry.RetryTemplate` (spring-batch-core 6.0.2 / spring-core 7.0.3)
