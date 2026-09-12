package rip.kill9.terminator.config.ex18;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaCursorItemReader;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.database.orm.JpaNamedQueryProvider;
import org.springframework.batch.infrastructure.item.database.orm.JpaQueryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ToxicPostExterminationConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job toxicPostExterminationJob(Step toxicPostExterminationStep) {
        return new JobBuilder(jobRepository)
            .start(toxicPostExterminationStep)
            .build();
    }

    @Bean
    public Step toxicPostExterminationStep(
        JpaPagingItemReader<Post> reportedPostReader,
        PostExterminationProcessor postExterminationProcessor,
        ItemWriter<ExterminatedPost> exterminatedPostItemWriter
    ) {
        return new StepBuilder(jobRepository)
            .<Post, ExterminatedPost>chunk(5)
            .transactionManager(transactionManager)
            .reader(reportedPostReader)
            .processor(postExterminationProcessor)
            .writer(exterminatedPostItemWriter)
            .build();
    }

//    @StepScope
//    @Bean
//    public JpaCursorItemReader<Post> reportedPostReader(
//        @Value("#{jobParameters['startDateTime']}") LocalDateTime startDateTime,
//        @Value("#{jobParameters['endDateTime']}") LocalDateTime endDateTime
//    ) {
//        return new JpaCursorItemReaderBuilder<Post>()
//            .name("reportedPostReader")
//            .entityManagerFactory(entityManagerFactory)
//            .queryString("""
//                  SELECT p FROM Post p JOIN FETCH p.reports r
//                  WHERE r.reportedAt >= :startDateTime AND r.reportedAt < :endDateTime
//                """)
//            .parameterValues(Map.of(
//                "startDateTime", startDateTime,
//                "endDateTime", endDateTime
//            ))
//            .build();
//    }

//    @StepScope
//    @Bean
//    public JpaCursorItemReader<Post> reportedPostReader(
//        @Value("#{jobParameters['startDateTime']}") LocalDateTime startDateTime,
//        @Value("#{jobParameters['endDateTime']}") LocalDateTime endDateTime
//    ) {
//        return new JpaCursorItemReaderBuilder<Post>()
//            .name("reportedPostReader")
//            .entityManagerFactory(entityManagerFactory)
//            .queryProvider(createQueryProvider())
//            .parameterValues(Map.of(
//                "startDateTime", startDateTime,
//                "endDateTime", endDateTime
//            ))
//            .build();
//    }
//
//    private JpaNamedQueryProvider<Post> createQueryProvider() {
//        JpaNamedQueryProvider<Post> queryProvider = new JpaNamedQueryProvider<>();
//        queryProvider.setEntityClass(Post.class);
//        queryProvider.setNamedQuery("Post.findByReportsReportedAtBetween");
//        return queryProvider;
//    }

    @StepScope
    @Bean
    public JpaPagingItemReader<Post> reportedPostReader(
        @Value("#{jobParameters['startDateTime']}") LocalDateTime startDateTime,
        @Value("#{jobParameters['endDateTime']}") LocalDateTime endDateTime
    ) {
        return new JpaPagingItemReaderBuilder<Post>()
            .name("reportedPostReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("""
                SELECT DISTINCT p FROM Post p
                JOIN p.reports r
                WHERE r.reportedAt >= :startDateTime AND r.reportedAt < :endDateTime
                ORDER BY p.id ASC
                """)
            .parameterValues(Map.of(
                "startDateTime", startDateTime,
                "endDateTime", endDateTime
            ))
            .pageSize(5)
            .transacted(false) // transacted=true 인 경우 reader에서 entityManager.flush()하기 때문에 reader에서 데이터 변경이 발생할 수 있음.
            .build();
    }

//    @Bean
//    public ItemWriter<ExterminatedPost> exterminatedPostItemWriter() {
//        return items -> {
//            items.forEach(blockedPost -> {
//                log.info("Exterminated: [ID:{}] '{}' by {} | 신고:{}건 | 점수:{} | kill-9 at {}",
//                    blockedPost.getPostId(), blockedPost.getTitle(), blockedPost.getWriter(),
//                    blockedPost.getReportCount(), String.format("%.2f", blockedPost.getScore()),
//                    blockedPost.getExterminatedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
//                );
//            });
//        };
//    }

    @Bean
    public JpaItemWriter<ExterminatedPost> exterminatedPostItemWriter() {
        return new JpaItemWriterBuilder<ExterminatedPost>()
            .entityManagerFactory(entityManagerFactory)
            .usePersist(true)
            .build();
    }

}
