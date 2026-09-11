# DeathSpringBatch6-OperationAuto

Spring Batch 스터디

## 예제

| 예제 | 설명 | 실행 명령 |
| --- | --- | --- |
| ex1 | System Termination 시뮬레이션 Job | `./gradlew bootRun --args='--spring.batch.job.name=systemTerminationSimulationJob'` |
| ex2 | 좀비 프로세스 정리 Job | `./gradlew bootRun --args='--spring.batch.job.name=zombieProcessCleanupJob'` |
| ex3 | 오래된 파일 삭제 Job | `./gradlew bootRun --args='--spring.batch.job.name=deleteOldFilesJob'` |
| ex5 | Job Parameter 예제 (시스템 봉쇄 Job) | `./gradlew bootRun --args="--spring.batch.job.name=systemLockdownJob targetFilePath=/test/a.csv,java.lang.String lockdownCount=666,java.lang.Integer"` |
| ex6 | Job Parameter 필드/생성자/세터 주입 예제 (시스템 침투 Job) | `./gradlew bootRun --args="--spring.batch.job.name=systemInfiltrationJob missionName=안산_데이터센터_침투,java.lang.String operationCommander=KILL-9,java.lang.String,false attackMethod=UNPLUG_CABLE,rip.kill9.terminator.config.ex6.SystemInfiltrationParameters\$AttackMethod"` |
| ex7 | DefaultJobParametersValidator 예제 (시스템 파괴 Job, destructionPower는 필수 / targetSystem은 선택) | `./gradlew bootRun --args='--spring.batch.job.name=systemDestructionJob destructionPower=9,java.lang.Long'` |
| ex8 | Job/Step 리스너 예제 (인터페이스 구현 방식 + `@BeforeJob`/`@AfterJob`/`@BeforeStep`/`@AfterStep` 애너테이션 방식) | `./gradlew bootRun --args='--spring.batch.job.name=securityAuditJob'` |
| ex9 | FlatFileItemReader 예제 (DelimitedLineTokenizer/FixedLengthTokenizer, 현재는 고정폭 방식 활성화) | `./gradlew bootRun --args="--spring.batch.job.name=systemFailureJob inputFile=/path/to/system_failures.txt,java.lang.String"` |
| ex10 | RegexLineTokenizer 예제 (정규식 캡처 그룹으로 로그 라인 파싱) | `./gradlew bootRun --args="--spring.batch.job.name=logAnalysisJob inputFile=/path/to/system_logs.txt,java.lang.String"` |
| ex11 | PatternMatchingCompositeLineMapper 예제 (라인 패턴별 다른 Tokenizer/FieldSetMapper 적용, BeanWrapperFieldSetMapper + RecordFieldSetMapper 혼용) | `./gradlew bootRun --args="--spring.batch.job.name=cursedCodeJob inputFile=/path/to/cursed_code.txt,java.lang.String"` |
| ex12 | MultiResourceItemReader 예제 (여러 CSV 파일을 파일명 역순 comparator로 순회하며 읽기) | `./gradlew bootRun --args="--spring.batch.job.name=multiResourceSystemFailureJob inputFilePath=/path/to/dir,java.lang.String"`<br>(디렉터리에 `critical-failures.csv`, `normal-failures.csv` 필요) |
| ex16 | JdbcCursorItemReader / JdbcPagingItemReader 예제 (H2 `victims` 테이블에서 조건부 조회, 현재는 페이징 방식 활성화, `schema.sql`/`data.sql`로 샘플 데이터 자동 적재) | `./gradlew bootRun --args='--spring.batch.job.name=terminatedVictimRecordJob'` |
| ex17 | JdbcPagingItemReader + ItemProcessor + JdbcBatchItemWriter 예제 (H2 `orders` 테이블에서 상태 불일치("탈취된") 주문을 찾아 상태 복구) | `./gradlew bootRun --args='--spring.batch.job.name=resecureJob'` |
| ex18 | JpaCursorItemReader → JpaNamedQueryProvider → JpaPagingItemReader 순으로 발전시킨 예제 (H2 `posts`/`reports` 테이블, 기간 내 신고된 게시물 조회 후 신고 점수 계산, 현재는 페이징 방식 활성화) | `./gradlew bootRun --args="--spring.batch.job.name=toxicPostExterminationJob startDateTime=2026-09-10T00:00:00,java.time.LocalDateTime endDateTime=2026-09-11T00:00:00,java.time.LocalDateTime"` |

## SQL 로그 확인

- JDBC 기반 예제(ex4, ex16, ex17 등): `application.yml`의 다음 설정으로 실제 SQL과 바인딩 파라미터를 볼 수 있습니다.
  ```yaml
  logging:
    level:
      org.springframework.jdbc.core: DEBUG
      org.springframework.jdbc.core.StatementCreatorUtils: TRACE
  ```
- JPA 기반 예제(ex18): `spring.jpa.show-sql`과 `hibernate.format_sql`/`highlight_sql` 설정으로 Hibernate가 생성하는 SQL을 보기 좋게 출력합니다. `spring.jpa.hibernate.ddl-auto`는 `none`으로 고정되어 있는데, 기본값(`create-drop`)을 쓰면 기동 시 Hibernate가 `schema.sql`/`data.sql`로 채워둔 `posts`/`reports` 테이블을 지우고 다시 만들어 데이터가 사라지기 때문입니다.

## JpaPagingItemReader 사용 시 주의점 (ex18)

`JpaPagingItemReader`는 OFFSET 기반 페이징으로 동작해서 아래와 같은 단점이 있습니다.

- **메모리 이슈**: 페이지 크기만큼 엔티티를 계속 영속성 컨텍스트에 올리므로 대량 데이터에서 메모리 부담이 커질 수 있음
- **실시간 데이터 변경에 취약**: 읽는 도중 데이터가 추가/삭제되면 OFFSET 기준이 밀려 항목을 건너뛰거나 중복 읽을 수 있음
- **fetch join 시 페이징 이슈**: `JOIN FETCH`로 컬렉션을 한 번에 당겨오면 페이징 쿼리가 애플리케이션 메모리에서 처리되어 성능 문제가 생기고, `WHERE`로 컬렉션을 필터링하면서 FETCH JOIN하면 연관 컬렉션에 조건에 맞는 일부만 채워지는 함정도 있음 (그래서 ex18은 `JOIN`만 쓰고 컬렉션은 `FetchType.EAGER` + `@BatchSize`로 별도 배치 조회)
- **N+1**: 연관관계를 지연 로딩하면 페이지의 각 엔티티마다 추가 쿼리가 나감 → `@BatchSize`로 N+1을 배치 단위 IN 쿼리로 줄임
- **`ORDER BY` 필수**: 페이지마다 동일한 순서를 보장해야 항목 누락/중복이 없음
- **`transacted=true`일 때의 flush 이슈**: `JpaPagingItemReader.doReadPage()`는 페이지를 읽은 직후 트랜잭션을 커밋하는데, `transacted=true`면 리더 안에서 `entityManager.flush()`가 발생해 리더 단계에서 의도치 않은 데이터 변경이 커밋될 수 있음 → ex18에서는 `transacted(false)`로 설정

이런 단점들 때문에 실무에서는 offset 기반 페이징 대신 no-offset(커서) 방식으로 개선한 리더를 쓰기도 합니다. 참고: [jojoldu/spring-batch-querydsl](https://github.com/jojoldu/spring-batch-querydsl)

## 참고자료

- https://github.com/KILL9-NO-MERCY/Death-Spring-Batch
