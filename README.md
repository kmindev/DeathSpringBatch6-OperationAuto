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
| ex18 | JpaCursorItemReader 예제 (H2 `posts`/`reports` 테이블, JPQL로 기간 내 신고된 게시물 조회 후 신고 점수 계산) | `./gradlew bootRun --args="--spring.batch.job.name=toxicPostExterminationJob startDateTime=2026-09-10T00:00:00,java.time.LocalDateTime endDateTime=2026-09-11T00:00:00,java.time.LocalDateTime"` |

## SQL 로그 확인

- JDBC 기반 예제(ex4, ex16, ex17 등): `application.yml`의 다음 설정으로 실제 SQL과 바인딩 파라미터를 볼 수 있습니다.
  ```yaml
  logging:
    level:
      org.springframework.jdbc.core: DEBUG
      org.springframework.jdbc.core.StatementCreatorUtils: TRACE
  ```
- JPA 기반 예제(ex18): `spring.jpa.show-sql`과 `hibernate.format_sql`/`highlight_sql` 설정으로 Hibernate가 생성하는 SQL을 보기 좋게 출력합니다. `spring.jpa.hibernate.ddl-auto`는 `none`으로 고정되어 있는데, 기본값(`create-drop`)을 쓰면 기동 시 Hibernate가 `schema.sql`/`data.sql`로 채워둔 `posts`/`reports` 테이블을 지우고 다시 만들어 데이터가 사라지기 때문입니다.

## 참고자료

- https://github.com/KILL9-NO-MERCY/Death-Spring-Batch
