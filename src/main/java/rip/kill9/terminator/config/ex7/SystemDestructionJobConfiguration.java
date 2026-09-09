package rip.kill9.terminator.config.ex7;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.config.Task;

@Configuration
public class SystemDestructionJobConfiguration {

    @Bean
    public Job systemDestructionJob(
        JobRepository jobRepository,
        Step systemDestructionStep,
        SystemDestructionValidator validator
    ) {
        return new JobBuilder(jobRepository)
            .validator(validator)
            .start(systemDestructionStep)
            .build();
    }

    @Bean
    public Step systemDestructionStep(
        JobRepository jobRepository,
        Tasklet systemDestructionTasklet
    ) {
        return new StepBuilder(jobRepository)
            .tasklet(systemDestructionTasklet)
            .build();
    }

}
