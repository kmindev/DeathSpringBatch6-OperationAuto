package rip.kill9.terminator.config.ex19;

import jakarta.persistence.Id;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.data.MongoCursorItemReader;
import org.springframework.batch.infrastructure.item.data.MongoItemWriter;
import org.springframework.batch.infrastructure.item.data.MongoPagingItemReader;
import org.springframework.batch.infrastructure.item.data.builder.MongoCursorItemReaderBuilder;
import org.springframework.batch.infrastructure.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.infrastructure.item.data.builder.MongoPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@AllArgsConstructor
public class HackerPatternDetectionJobConfiguration {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final MongoTemplate mongoTemplate;

    @Bean
    public Job hackerPatternDetectionJob(Step hackerPatternDetectionStep) {
        return new JobBuilder(jobRepository)
            .start(hackerPatternDetectionStep)
            .build();
    }

    @Bean
    public Step hackerPatternDetectionStep(
        MongoPagingItemReader<SecurityLog> securityLogReader,
        ItemProcessor<SecurityLog, SecurityLog> hackerPatternAnalyzeProcessor,
        // MongoItemWriter 추가
        MongoItemWriter<SecurityLog> securityLogWriter
    ) {
        return new StepBuilder(jobRepository)
            .<SecurityLog, SecurityLog>chunk(10)
            .transactionManager(transactionManager)
            .reader(securityLogReader)
            .processor(hackerPatternAnalyzeProcessor)
            .writer(securityLogWriter)
            .build();
    }

    // 1. MongoCursorItemReader + jsonQuery 방식
//    @Bean
//    @StepScope
//    public MongoCursorItemReader<SecurityLog> securityLogReader(
//        @Value("#{jobParameters['searchDate']}") LocalDate searchDate
//    ) {
//        Date startOfDay = Date.from(searchDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
//        Date endOfDay = Date.from(
//            searchDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
//
//        return new MongoCursorItemReaderBuilder<SecurityLog>()
//            .name("securityLogReader")
//            .template(mongoTemplate)
//            .collection("security_logs")
//            .jsonQuery("""
//                    {
//                        "label": "PENDING_ANALYSIS",
//                        "timestamp": {
//                            "$gte": ?0,
//                            "$lt": ?1
//                        }
//                    }
//                    """)
//            .parameterValues(List.of(startOfDay, endOfDay))
//            .sorts(Map.of("timestamp", Sort.Direction.ASC))
//            .targetType(SecurityLog.class)
//            .batchSize(10)
//            .build();
//    }

    // 2. MongoCursorItemReader + Query/Criteria 방식
//    @Bean
//    @StepScope
//    public MongoCursorItemReader<SecurityLog> securityLogReader(
//        @Value("#{jobParameters['searchDate']}") LocalDate searchDate
//    ) {
//        Query query = new Query()
//            .addCriteria(Criteria.where("label").is("PENDING_ANALYSIS"))
//            .addCriteria(Criteria.where("timestamp")
//                .gte(Date.from(searchDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
//                .lt(Date.from(searchDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())))
//            .with(Sort.by(Direction.ASC, "timestamp"))
//            .cursorBatchSize(10);
//
//        return new MongoCursorItemReaderBuilder<SecurityLog>()
//            .name("securityLogReader")
//            .template(mongoTemplate)
//            .collection("security_logs")
//            .query(query)
//            .sorts(Map.of("timestamp", Direction.ASC)) // 빌더 버그 때문에 필요함
//            .targetType(SecurityLog.class)
//            .batchSize(10)
//            .build();
//    }

    // 3. MongoPagingItemReader + Query/Criteria 방식 (최종)
    @Bean
    @StepScope
    public MongoPagingItemReader<SecurityLog> securityLogReader(
        @Value("#{jobParameters['searchDate']}") LocalDate searchDate
    ) {
        Query query = new Query()
            .addCriteria(Criteria.where("label").is("PENDING_ANALYSIS"))
            .addCriteria(Criteria.where("timestamp")
                .gte(Date.from(searchDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .lt(Date.from(searchDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())))
            .with(Sort.by(Direction.ASC, "timestamp"));

        return new MongoPagingItemReaderBuilder<SecurityLog>()
            .name("securityLogReader")
            .template(mongoTemplate)
            .collection("security_logs")
            .query(query)
            .sorts(Map.of("timestamp", Direction.ASC)) // 빌더 버그 때문에 필요함
            .targetType(SecurityLog.class)
            .pageSize(10)
            .build();
    }

    @Bean
    public ItemProcessor<SecurityLog, SecurityLog> hackerPatternAnalyzeProcessor() {
        return securityLog -> {
            String detectedPattern = analyzeAttackPattern(securityLog);
            securityLog.setLabel(detectedPattern);
            return securityLog;
        };
    }

    private String analyzeAttackPattern(SecurityLog securityLog) {
        String command = securityLog.getCommand().toLowerCase();

        // Lateral Movement 탐지
        if (command.contains("ssh") || command.contains("telnet")) {
            return "Lateral_Movement";
        }

        // Privilege Escalation 탐지
        if (command.contains("sudo") || command.contains("su ")) {
            return "Privilege_Escalation";
        }

        // Defense Evasion 탐지
        if (command.contains("history -c") || command.contains("rm /var/log") || command.contains("killall rsyslog")) {
            return "Defense_Evasion";
        }

        return "UNKNOWN";
    }

    @Bean
    public MongoItemWriter<SecurityLog> securityLogWriter() {
        return new MongoItemWriterBuilder<SecurityLog>()
            .template(mongoTemplate)
            .collection("security_logs")
            .mode(MongoItemWriter.Mode.UPSERT)  // 기존 문서 수정
            .build();
    }

    @Document
    @Data
    public static class SecurityLog {
        @Id
        private String id;
        private String attackerId;
        private String command;
        private LocalDateTime timestamp;
        private String label;
    }
}
