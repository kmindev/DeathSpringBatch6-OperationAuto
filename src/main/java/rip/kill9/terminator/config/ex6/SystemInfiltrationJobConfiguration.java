package rip.kill9.terminator.config.ex6;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SystemInfiltrationJobConfiguration {

    @Bean
    public Job systemInfiltrationJob(JobRepository jobRepository, Step infiltrationStep) {
        return new JobBuilder(jobRepository)
            .start(infiltrationStep)
            .build();
    }

    @Bean
    public Step infiltrationStep(JobRepository jobRepository, Tasklet infiltrationTasklet) {
        return new StepBuilder(jobRepository)
            .tasklet(infiltrationTasklet)
            .build();
    }

    @Bean
    @StepScope
    public Tasklet infiltrationTasklet(SystemInfiltrationParameters infiltrationParameters) {
        return ((contribution, chunkContext) -> {
            log.info("시스템 침투 작전 초기화!");
            log.info("임무 코드 네임: {}", infiltrationParameters.getMissionName());
            log.info("공격 방법: {}", infiltrationParameters.getAttackMethod());
            log.info("작전 지휘관: {}", infiltrationParameters.getOperationCommander());

            String result = switch (infiltrationParameters.getAttackMethod()) {
                case UNPLUG_CABLE -> "선 뽑기 완료. 시스템 즉시 종료됨.";
                case HAMMER_SMASH -> "물리적 타격. 하트웨어 파괴 성공.";
                case COFFEE_SPILL -> "커피 침투. 단락으로 시스템 마비.";
                case CTRL_ALT_DELETE -> "키보드 난타. 시스템 응답 없음.";
            };

            log.info(result);
            log.info("{} 작전 성공!", infiltrationParameters.getAttackMethod());

            return RepeatStatus.FINISHED;
        });
    }
}
