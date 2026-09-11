package rip.kill9.terminator.config.ex16;

import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class TerminatedVictimRecordConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Bean
    public Job terminatedVictimRecordJob(Step terminatedVictimRecordStep) {
        return new JobBuilder(jobRepository)
            .start(terminatedVictimRecordStep)
            .build();
    }

    @Bean
    public Step terminatedVictimRecordStep(
        JdbcCursorItemReader<Victim> terminatedVictimReader
    ) {
        return new StepBuilder(jobRepository)
            .<Victim, Victim>chunk(5)
            .transactionManager(transactionManager)
            .reader(terminatedVictimReader)
            .writer(items -> items.forEach(victim -> log.info("victim: {}", victim)))
            .build();
    }

    @Bean
    public JdbcCursorItemReader<Victim> terminatedVictimReader() {
        return new JdbcCursorItemReaderBuilder<Victim>()
            .name("terminatedVictimReader")
            .dataSource(dataSource)
            .sql("SELECT * FROM victims WHERE status = ? AND terminated_at <= ?")
            .queryArguments(List.of("TERMINATED", LocalDateTime.now()))
            .beanRowMapper(Victim.class)
            .build();
    }

    @NoArgsConstructor
    @Data
    private static class Victim {
        private Long id;
        private String name;
        private String processId;
        private LocalDateTime terminatedAt;
        private String status;
    }
}
