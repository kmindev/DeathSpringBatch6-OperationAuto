package rip.kill9.terminator.config.ex5;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SystemLockdownJobConfiguration {

    @Bean
    public Job systemLockdownJob(JobRepository jobRepository, Step lockdownStep) {
        return new JobBuilder(jobRepository)
            .start(lockdownStep)
            .build();
    }

    @Bean
    public Step lockdownStep(JobRepository jobRepository, Tasklet chmod000Tasklet) {
        return new StepBuilder(jobRepository)
            .tasklet(chmod000Tasklet)
            .build();
    }

    @Bean
    @StepScope
    public Tasklet chmod000Tasklet(
        @Value("#{jobParameters['targetFilePath']}") String targetFilePath,
        @Value("#{jobParameters['lockdownCount']}") Integer lockdownCount
    ) {
        return ((contribution, chunkContext) -> {
            log.info("시스템 봉쇄 작전 개시");
            log.info("타깃 파일 경로: {}", targetFilePath);
            log.info("봉쇄 대상: {} 개", lockdownCount);
            log.info("chmod 000 실행 중...");
            log.info(" 접근 불가. 탈출 불가. 시스템 종결.");
            return RepeatStatus.FINISHED;
        });
    }
}
