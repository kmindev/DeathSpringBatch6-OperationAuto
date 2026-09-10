package rip.kill9.terminator.config.ex9;

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
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.transform.Range;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class SystemFailureJobConfiguration {

    private final JobRepository jobRepository;

    @Bean
    public Job systemFailureJob(Step systemFailuresStep) {
        return new JobBuilder(jobRepository)
            .start(systemFailuresStep)
            .build();
    }

    @Bean
    public Step systemFailuresStep(
        FlatFileItemReader<SystemFailure> systemFailureFlatFileItemReader,
        ItemWriter<SystemFailure> systemFailureItemWriter
    ) {
        return new StepBuilder(jobRepository)
            .<SystemFailure, SystemFailure>chunk(10)
            .reader(systemFailureFlatFileItemReader)
            .writer(systemFailureItemWriter)
            .build();
    }

    // 구분가 기반 FlatFileItemReader
//    @Bean
//    @StepScope
//    public FlatFileItemReader<SystemFailure> systemFailureFlatFileItemReader(
//        @Value("#{jobParameters['inputFile']}") String inputFile
//    ) {
//        return new FlatFileItemReaderBuilder<SystemFailure>()
//            .name("systemFailureItemReader")
//            .resource(new FileSystemResource(inputFile))
//            .delimited() // DelimitedLineTokenizer 사용하겠다는 설정(구분자로 데이터를 토큰화)
//            .delimiter(",")
//            .names("errorId", "errorDateTime", "severity", "processId", "errorMessage")// 순서대로 토큰과 1:1 매핑
//            .targetType(SystemFailure.class)
//            .linesToSkip(1) // 첫 번재 줄은 컬럼명(헤더)에 해당되므로 건너뛴다. (주석['#']도 기본적으로 건너뜀)
//            .strict(true) // 검증 강도 설정 (파일 존재, 토큰 길이 등)
//            .build();
//    }

    @Bean
    @StepScope
    public FlatFileItemReader<SystemFailure> systemFailureFlatFileItemReader(
        @Value("#{jobParameters['inputFile']}") String inputFile
    ) {
        return new FlatFileItemReaderBuilder<SystemFailure>()
            .name("systemFailureItemReader")
            .resource(new FileSystemResource(inputFile))
            .fixedLength() // FixedLengthTokenizer를 사용하겠다는 설정
            .columns(new Range[] {
                new Range(1, 8),
                new Range(9, 29),
                new Range(30, 39),
                new Range(40, 45),
                new Range(46, 66)
            })
            .names("errorId", "errorDateTime", "severity", "processId", "errorMessage")
            .targetType(SystemFailure.class)
            .customEditors(Map.of(LocalDateTime.class, dateTimeEditor()))
            .build();
    }

    @Bean
    public ItemWriter<SystemFailure> systemFailureItemWriter(){
        return chunk -> {
            for(SystemFailure systemFailure : chunk) {
                log.info("Processing system failure: {}", systemFailure);
            }
        };
    }

    private PropertyEditor dateTimeEditor() {
        return new PropertyEditorSupport() {
          @Override
          public void setAsText(String text) {
              DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
              setValue(LocalDateTime.parse(text, formatter));
          }
        };
    }

}
