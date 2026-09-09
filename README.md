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

## 참고자료

- https://github.com/KILL9-NO-MERCY/Death-Spring-Batch
