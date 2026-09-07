package rip.kill9.terminator.config.ex4;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class DeleteOldRecordsJobConfiguration {

    private final JobRepository jobRepository;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public Step deleteOldRecordsStep() {
        return new StepBuilder(jobRepository)
            .tasklet(((contribution, chunkContext) -> {
                int deleted = jdbcTemplate.update("DELETE FROM logs WHERE created < NOW() - INTERVAL 7 DAY");
                log.info("{}개의 오래된 레코드가 삭제됐습니다.", deleted);
                return RepeatStatus.FINISHED;
            }))
            .build();
    }

}
