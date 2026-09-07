package rip.kill9.terminator.config.ex2;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class ZombieProcessCleanupJobConfiguration {

    private final JobRepository jobRepository;

    @Bean
    public Tasklet zombieProcessCleanupTasklet() {
        return new ZombieProcessCleanupTasklet();
    }

    @Bean
    public Step zombieProcessCleanupStep(Tasklet zombieProcessCleanupTasklet) {
        return new StepBuilder(jobRepository)
            .tasklet(zombieProcessCleanupTasklet)
            .build();
    }

    @Bean
    public Job zombieProcessCleanupJob(Step zombieProcessCleanupStep) {
        return new JobBuilder(jobRepository)
            .start(zombieProcessCleanupStep)
            .build();
    }

}
