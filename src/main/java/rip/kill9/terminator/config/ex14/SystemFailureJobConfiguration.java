package rip.kill9.terminator.config.ex14;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.separator.JsonRecordSeparatorPolicy;
import org.springframework.batch.infrastructure.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.infrastructure.item.json.JacksonJsonObjectReader;
import org.springframework.batch.infrastructure.item.json.JsonFileItemWriter;
import org.springframework.batch.infrastructure.item.json.JsonItemReader;
import org.springframework.batch.infrastructure.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.infrastructure.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import rip.kill9.terminator.config.ex13.DeathNoteWriteJobConfiguration.DeathNote;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class SystemFailureJobConfiguration {
    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Bean
    public Job systemFailureJob(Step systemFailureStep) {
        return new JobBuilder(jobRepository)
            .start(systemFailureStep)
            .build();
    }

    @Bean
    public Step systemFailureStep(JsonItemReader<SystemFailure> systemFailureFlatFileItemReader) {
        return new StepBuilder(jobRepository)
            .<SystemFailure, SystemFailure>chunk(10)
            .reader(systemFailureFlatFileItemReader)
            .writer(items -> items.forEach(item -> log.info("{}", item)))
            .build();
    }

    // 한계: 하나의 라인에 json이 잘정리되면 문제가 없지만, 멀티라인 json인 경우 불가능.
//    @Bean
//    @StepScope
//    public FlatFileItemReader<SystemFailure> systemFailureFlatFileItemReader(
//        @Value("#{jobParameters['inputFile']}") String inputFile) {
//        return new FlatFileItemReaderBuilder<SystemFailure>()
//            .name("systemFailureStep")
//            .resource(new FileSystemResource(inputFile))
//            .lineMapper(((line, lineNumber) -> objectMapper.readValue(line, SystemFailure.class)))
//            .build();
//    }

//    @Bean
//    @StepScope
//    public FlatFileItemReader<SystemFailure> systemFailureFlatFileItemReader(
//        @Value("#{jobParameters['inputFile']}") String inputFile) {
//        return new FlatFileItemReaderBuilder<SystemFailure>()
//            .name("systemFailureStep")
//            .resource(new FileSystemResource(inputFile))
//            .lineMapper(((line, lineNumber) -> objectMapper.readValue(line, SystemFailure.class)))
//            .recordSeparatorPolicy(new JsonRecordSeparatorPolicy()) // JSON 경계 감지기
//            .build();
//    }

    @Bean
    @StepScope
    public JsonItemReader<SystemFailure> systemFailureFlatFileItemReader(@Value("#{jobParameters['inputFile']}") String inputFile) {
        return new JsonItemReaderBuilder<SystemFailure>()
            .jsonObjectReader(new JacksonJsonObjectReader<>(SystemFailure.class))
            .name("systemFailureStep")
            .resource(new FileSystemResource(inputFile))
            .build();
    }

    @Bean
    @StepScope
    public JsonFileItemWriter<DeathNote> systemFailureFlatFileItemWriter(@Value("#{jobParameters['outputPath']}") String outputPath) {
        return new JsonFileItemWriterBuilder<DeathNote>()
            .jsonObjectMarshaller(new JacksonJsonObjectMarshaller<>())
            .resource(new FileSystemResource(outputPath + "/death_notes.json"))
            .name("logEntryJsonWriter")
            .build();
    }

    public record SystemFailure(String command, int cpu, String status) {

    }

}
