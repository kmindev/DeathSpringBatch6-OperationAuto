package rip.kill9.terminator.config.ex20;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.redis.RedisItemReader;
import org.springframework.batch.infrastructure.item.redis.RedisItemWriter;
import org.springframework.batch.infrastructure.item.redis.builder.RedisItemReaderBuilder;
import org.springframework.batch.infrastructure.item.redis.builder.RedisItemWriterBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;
import rip.kill9.terminator.config.ex20.AttackModels.AttackLog;

@Slf4j
@Configuration
public class HackerAttackReportJobConfiguration {
    private final JobRepository jobRepository;
    private final RedisTemplate<String, AttackLog> redisTemplate;

    public HackerAttackReportJobConfiguration(
        JobRepository jobRepository,
        RedisConnectionFactory redisConnectionFactory
    ) {
        this.jobRepository = jobRepository;

        RedisTemplate<String, AttackLog> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JacksonJsonRedisSerializer<>(AttackLog.class));
        redisTemplate.afterPropertiesSet();

        this.redisTemplate = redisTemplate;
    }

    @Bean
    public Job hackerAttackReportJob(
        Step aggregateHackerAttackStep,
        Step reportHackerAttacktStep
    ) {
        return new JobBuilder(jobRepository)
            .start(aggregateHackerAttackStep)
            .next(reportHackerAttacktStep)
            .build();
    }

    @Bean
    public Step aggregateHackerAttackStep(
        RedisItemReader<String, AttackLog> attackLogReader,
        AttackCounter attackCounter,
        RedisItemWriter<String, AttackLog> deleteAttackLogWriter
    ) {
        return new StepBuilder(jobRepository)
            .<AttackLog, AttackLog>chunk(10)
            .reader(attackLogReader)
            .processor(attackLog -> {
                log.info("{}", attackLog);
                attackCounter.record(attackLog);
                return attackLog;
            })
            .writer(deleteAttackLogWriter)
            .build();
    }

    @Bean
    public Step reportHackerAttacktStep(AttackCounter attackCounter) {
        return new StepBuilder(jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("{}", attackCounter.generateAnalysis());
                return RepeatStatus.FINISHED;
            })
            .build();
    }

    @Bean
    public RedisItemReader<String, AttackLog> attackLogReader() {
        return new RedisItemReaderBuilder<String, AttackLog>()
            .redisTemplate(redisTemplate)
            .scanOptions(ScanOptions.scanOptions()
                .match("attack:*")  // attack: 으로 시작하는 키만 스캔
                .count(10)
                .build())
            .build();
    }

    @Bean
    public RedisItemWriter<String, AttackLog> deleteAttackLogWriter() {
        return new RedisItemWriterBuilder<String, AttackLog>()
            .redisTemplate(redisTemplate)
            .itemKeyMapper(attackLog -> "attack:" + attackLog.getId()) // 키 구성
            .delete(true) // DEL 명령사용하여 삭제, false일 경우 저장
            .build();
    }

//    @Component
//    @RequiredArgsConstructor
//    public static class AggregateHackerAttackItemWriter implements ItemWriter<AttackLog> {
//        private final AttackCounter attackCounter;
//
//        @Override
//        public void write(Chunk<? extends AttackLog> chunk) {
//            for (AttackLog attackLog : chunk) {
//                log.info("{}", attackLog);
//                attackCounter.record(attackLog);
//            }
//        }
//    }
}
