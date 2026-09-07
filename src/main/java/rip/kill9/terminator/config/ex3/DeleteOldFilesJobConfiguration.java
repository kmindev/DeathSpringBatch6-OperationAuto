package rip.kill9.terminator.config.ex3;

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
public class DeleteOldFilesJobConfiguration {

    private final JobRepository jobRepository;

    @Bean
    public Tasklet deleteOldFilesTasklet() {
        return new DeleteOldFilesTasklet("/path/to/temp", 30);
    }

    @Bean
    public Step deleteOldFilesStep(Tasklet deleteOldFilesTasklet) {
        return new StepBuilder(jobRepository)
            .tasklet(deleteOldFilesTasklet)
            .build();
    }

    @Bean
    public Job deleteOldFilesJob(Step deleteOldFilesStep) {
        return new JobBuilder(jobRepository)
            .start(deleteOldFilesStep)
            .build();
    }
}
