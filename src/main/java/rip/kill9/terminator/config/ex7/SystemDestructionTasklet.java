package rip.kill9.terminator.config.ex7;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SystemDestructionTasklet implements Tasklet {
    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        JobParameters jobParameters = chunkContext.getStepContext().getStepExecution().getJobParameters();

        Long destructionPower = jobParameters.getLong("destructionPower");

        log.info("파괴 레벨: {}", destructionPower);

        return RepeatStatus.FINISHED;
    }
}
