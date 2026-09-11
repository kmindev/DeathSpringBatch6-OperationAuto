package rip.kill9.terminator.config.ex17;

import java.time.LocalDateTime;
import java.util.Map;
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
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class HijackedOrderResecureJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Bean
    public Job resecureJob(Step resecureHijackedOrderStep) {
        return new JobBuilder(jobRepository)
            .start(resecureHijackedOrderStep)
            .build();
    }

    @Bean
    public Step resecureHijackedOrderStep(
        JdbcPagingItemReader<HijackedOrder> hijackedOrderReader,
        ItemProcessor<HijackedOrder, HijackedOrder> orderRescueProcessor,
        JdbcBatchItemWriter<HijackedOrder> rescuedOrderWriter
    ) {
        return new StepBuilder(jobRepository)
            .<HijackedOrder, HijackedOrder>chunk(10)
            .transactionManager(transactionManager)
            .reader(hijackedOrderReader)
            .processor(orderRescueProcessor)
            .writer(rescuedOrderWriter)
            .build();
    }

    @Bean
    public JdbcPagingItemReader<HijackedOrder> hijackedOrderReader() throws Exception {
        return new JdbcPagingItemReaderBuilder<HijackedOrder>()
            .name("compromisedOrderReader")
            .dataSource(dataSource)
            .pageSize(10)
            .selectClause("SELECT id, customer_id, order_datetime AS orderDateTime, status, shipping_id AS shippingId")
            .fromClause("FROM orders")
            .whereClause("""
                WHERE (status = 'SHIPPED' AND shipping_id IS NULL)
                OR (status = 'CANCELLED' AND shipping_id IS NOT NULL)
                """)
            .sortKeys(Map.of("id", Order.ASCENDING))
            .beanRowMapper(HijackedOrder.class)
            .build();
    }

    @Bean
    public ItemProcessor<HijackedOrder, HijackedOrder> orderRescueProcessor() {
        return order -> {
            if(order.getShippingId() == null) {
                order.setStatus("READY_FOR_SHIPMENT");
            } else {
                order.setStatus("SHIPPED");
            }
            log.info("rescued order: {}", order);
            return order;
        };
    }

    @Bean
    public JdbcBatchItemWriter<HijackedOrder> rescuedOrderWriter() {
        return new JdbcBatchItemWriterBuilder<HijackedOrder>()
            .dataSource(dataSource)
            .sql("UPDATE orders SET status = :status WHERE id = :id")
            .beanMapped()
            .assertUpdates(true) // 단 하나의 데이터라도 업데이트(또는 추가)에 실패하면 즉시 예외를 던져 작전을 중단
            .build();
    }

    @Data
    @NoArgsConstructor
    public static class HijackedOrder {
        private Long id;
        private Long customerId;
        private LocalDateTime orderDateTime;
        private String status;
        private String shippingId;
    }
}
