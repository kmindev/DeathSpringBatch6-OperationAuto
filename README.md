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

## 참고자료

- https://github.com/KILL9-NO-MERCY/Death-Spring-Batch
