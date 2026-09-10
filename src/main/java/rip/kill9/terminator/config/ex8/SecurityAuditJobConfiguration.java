package rip.kill9.terminator.config.ex8;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SecurityAuditJobConfiguration {

    @Bean
    public Job securityAuditJob(
        JobRepository jobRepository,
        Step penetrationTestStep
    ) {
        return new JobBuilder(jobRepository)
            .listener(new SecurityAuditJobExecutionListener())
            .start(penetrationTestStep)
            .build();
    }

    @Bean
    public Step penetrationTestStep(JobRepository jobRepository) {
        return new StepBuilder(jobRepository)
            .listener(new SecurityAuditStepExecutionListener())
            .tasklet(((contribution, chunkContext) -> {
                log.info("[IN STEP] 취약점 분석 진행 중");
                return RepeatStatus.FINISHED;
            }))
            .build();
    }

}
