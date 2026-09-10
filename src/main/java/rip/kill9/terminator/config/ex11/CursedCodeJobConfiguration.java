package rip.kill9.terminator.config.ex11;

import java.util.HashMap;
import java.util.Map;
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
import org.springframework.batch.infrastructure.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.infrastructure.item.file.mapping.FieldSetMapper;
import org.springframework.batch.infrastructure.item.file.mapping.PatternMatchingCompositeLineMapper;
import org.springframework.batch.infrastructure.item.file.mapping.RecordFieldSetMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.LineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CursedCodeJobConfiguration {

    private final JobRepository jobRepository;

    @Bean
    public Job cursedCodeJob(Step cursedCodeStep) {
        return new JobBuilder(jobRepository)
            .start(cursedCodeStep)
            .build();
    }

    @Bean
    public Step cursedCodeStep(FlatFileItemReader<CursedCode> cursedCodeReader) {
        return new StepBuilder(jobRepository)
            .<CursedCode, CursedCode>chunk(10)
            .reader(cursedCodeReader)
            .writer(chunk -> chunk.getItems().forEach(item -> log.info("{}", item)))
            .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<CursedCode> cursedCodeReader(
        @Value("#{jobParameters['inputFile']}") String inputFile
    ) {
        return new FlatFileItemReaderBuilder<CursedCode>()
            .name("cursedCodeReader")
            .resource(new FileSystemResource(inputFile))
            .lineMapper(cursedCodeLimeMapper())
            .build();
    }

    public PatternMatchingCompositeLineMapper<CursedCode> cursedCodeLimeMapper() {
        Map<String, LineTokenizer> tokenizers = new HashMap<>();
        tokenizers.put("LEGACY*", legacyLineTokenizer());
        tokenizers.put("HAUNTED*", hauntedLineTokenizer());

        Map<String, FieldSetMapper<CursedCode>> mappers = new HashMap<>();
        mappers.put("LEGACY*", legacyFieldSetMapper());
        mappers.put("HAUNTED*", hauntedFieldSetMapper());
        return new PatternMatchingCompositeLineMapper<>(tokenizers, mappers);
    }

    private DelimitedLineTokenizer legacyLineTokenizer() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(",");
        tokenizer.setNames("type", "filename", "writtenDate", "curseLevel", "description");
        return tokenizer;
    }

    private DelimitedLineTokenizer hauntedLineTokenizer() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(",");
        tokenizer.setNames("type", "filename", "authorId", "lastCommit", "docStatus");
        return tokenizer;
    }

    private BeanWrapperFieldSetMapper<CursedCode> legacyFieldSetMapper() {
        BeanWrapperFieldSetMapper<CursedCode> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(LegacyCode.class);
        return mapper;
    }

    private FieldSetMapper<CursedCode> hauntedFieldSetMapper() {
        RecordFieldSetMapper<HauntedCode> mapper = new RecordFieldSetMapper<>(HauntedCode.class);
        return mapper::mapFieldSet;
    }

}
