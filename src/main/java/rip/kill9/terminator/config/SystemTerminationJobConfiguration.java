package rip.kill9.terminator.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SystemTerminationJobConfiguration {

    private int processKilled = 0;
    private final int TERMINATION_TARGET = 5;

    private final JobRepository jobRepository;

    @Bean
    public Job systemTerminationSimulationJob(
        Step enterWorldStep,
        Step meetNPCStep,
        Step defeatProcessStep,
        Step completeQuestStep
    ) {
        return new JobBuilder(jobRepository)
            .start(enterWorldStep)
            .next(meetNPCStep)
            .next(defeatProcessStep)
            .next(completeQuestStep)
            .build();
    }

    @Bean
    public Step enterWorldStep() {
        return new StepBuilder(jobRepository)
            .tasklet((contribution, chunkContext) -> {
                System.out.println("System Termination 시뮬에이션 세계에 접속했습니다!");
                return RepeatStatus.FINISHED;
            }).build();
    }

    @Bean
    public Step meetNPCStep() {
        return new StepBuilder(jobRepository)
            .tasklet((contribution, chunkContext) -> {
                System.out.println("시스템 관리자 NPC를 만났습니다.");
                System.out.println("첫 번째 미션: 좀비 프로세스 " + TERMINATION_TARGET + "개 처형하기");
                return RepeatStatus.FINISHED;
            }).build();
    }

    @Bean
    public Step defeatProcessStep() {
        return new StepBuilder(jobRepository)
            .tasklet(((contribution, chunkContext) -> {
                int terminated = processKilled++;
                System.out.println("좀비 프로세스 처형 완료! (현재 " + terminated + "/" + TERMINATION_TARGET + ")");
                if (terminated < TERMINATION_TARGET) {
                    return RepeatStatus.CONTINUABLE;
                } else {
                    return RepeatStatus.FINISHED;
                }
            })).build();
    }

    @Bean
    public Step completeQuestStep() {
        return new StepBuilder(jobRepository)
            .tasklet(((contribution, chunkContext) -> {
                System.out.println("미션 완료! 좀비 프로세스 " + TERMINATION_TARGET + "개 처형 성공!");
                System.out.println("보상: KILL -9 권한 획득, 시스템 제어 레벨 1 달성");
                return  RepeatStatus.FINISHED;
            })).build();
    }

}
