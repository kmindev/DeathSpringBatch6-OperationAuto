package rip.kill9.terminator.config.ex10;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.transform.RegexLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class LogAnalysisJobConfiguration {

    private final JobRepository jobRepository;

    @Bean
    public Job logAnalysisJob(Step logAnalysisStep) {
        return new JobBuilder(jobRepository)
            .start(logAnalysisStep)
            .build();
    }

    @Bean
    public Step logAnalysisStep(
        FlatFileItemReader<LogEntry> logItemReader,
        ItemWriter<LogEntry> logItemWriter
    ) {
        return new StepBuilder(jobRepository)
            .<LogEntry, LogEntry>chunk(10)
            .reader(logItemReader)
            .writer(logItemWriter)
            .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<LogEntry> logItemReader(
        @Value("#{jobParameters['inputFile']}") String inputFile
    ) {
        RegexLineTokenizer tokenizer = new RegexLineTokenizer("\\[\\w+\\]\\[Thread-(\\d+)\\]\\[CPU: \\d+%\\] (.+)");

        return new FlatFileItemReaderBuilder<LogEntry>()
            .name("logItemReader")
            .resource(new FileSystemResource(inputFile))
            .lineTokenizer(tokenizer)
            .fieldSetMapper(fieldSet -> new LogEntry(fieldSet.readString(0), fieldSet.readRawString(1)))
            .build();
    }

    @Bean
    public ItemWriter<LogEntry> logItemWriter() {
            return items -> {
                for (LogEntry logEntry : items) {
                    log.info("THD-{}: {}", logEntry.threadNum(), logEntry.message());
                }
            };
    }

}
