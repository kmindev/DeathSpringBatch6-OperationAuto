package rip.kill9.terminator.config.ex12;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
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
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.MultiResourceItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import rip.kill9.terminator.config.ex9.SystemFailure;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class MultiResourceItemReaderSystemFailureJobConfiguration {

    private final JobRepository jobRepository;

    @Bean
    public Job multiResourceSystemFailureJob(Step systemFailureStep) {
        return new JobBuilder(jobRepository)
            .start(systemFailureStep)
            .build();
    }

    @Bean
    public ItemWriter<SystemFailure> systemFailureStdoutItemWriter() {
        return chunk -> {
            for (SystemFailure systemFailure : chunk) {
                log.info("Processing system failure: {}", systemFailure);
            }
        };
    }

    @Bean
    public Step systemFailureStep(
        MultiResourceItemReader<SystemFailure> multiSystemFailureItemReader,
        ItemWriter<SystemFailure> systemFailureStdoutItemWriter
    ) {
        return new StepBuilder(jobRepository)
            .<SystemFailure, SystemFailure>chunk(10)
            .reader(multiSystemFailureItemReader)
            .writer(systemFailureStdoutItemWriter)
            .build();
    }

    @Bean
    @StepScope
    public MultiResourceItemReader<SystemFailure> multiSystemFailureItemReader(
        @Value("#{jobParameters['inputFilePath']}") String inputFilePath
    ) {
        return new MultiResourceItemReaderBuilder<SystemFailure>()
            .name("multiSystemFailureItemReader")
            .resources(
                new FileSystemResource(inputFilePath + "/critical-failures.csv"),
                new FileSystemResource(inputFilePath + "/normal-failures.csv"))
            .comparator((r1, r2) -> r2.getFilename().compareTo(r1.getFilename())) // 파일명 역순으로 정렬해서 읽기 (기본은 알파벨 순서로 읽음)
            .delegate(systemFailureFlatFileItemReader())
            .build();
    }

    public FlatFileItemReader<SystemFailure> systemFailureFlatFileItemReader() {
        return new FlatFileItemReaderBuilder<SystemFailure>()
            .name("systemFailureFlatFileItemReader")
            .delimited()
            .delimiter(",")
            .names("errorId", "errorDateTime", "severity", "processId", "errorMessage")
            .targetType(SystemFailure.class)
            .customEditors(Map.of(LocalDateTime.class, dateTimeEditor()))
            .linesToSkip(1)
            .build();
    }

    private PropertyEditor dateTimeEditor() {
        return new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        };
    }
}
