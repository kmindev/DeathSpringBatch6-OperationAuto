package rip.kill9.terminator.config.ex13;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.MultiResourceItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class DeathNoteWriteJobConfiguration {

    @Bean
    public Job deathNoteWriteJob(
        JobRepository jobRepository,
        Step deathNoteWriteStep
    ) {
        return new JobBuilder(jobRepository)
            .start(deathNoteWriteStep)
            .build();
    }

    @Bean
    public Step deathNoteWriteStep(
        JobRepository jobRepository,
        ListItemReader<DeathNote> deathNoteListReader,
        MultiResourceItemWriter<DeathNote> multiResourceDeathNoteWriter
    ) {
        return new StepBuilder(jobRepository)
            .<DeathNote, DeathNote>chunk(10)
            .reader(deathNoteListReader)
            .writer(multiResourceDeathNoteWriter)
            .build();
    }

    @Bean
    public ListItemReader<DeathNote> deathNoteListReader() {
        List<DeathNote> victims = List.of(
            new DeathNote("THRD-7421", "DeadLock-Alpha-1", "교착상태 처형", "SIGTERM"),
            new DeathNote("THRD-0512", "DeadLock-Alpha-2", "가출 스레드 처형", "KILL-9"),
            new DeathNote("THRD-0x00", "DeadLock-Alpha-3", "날뛰는 포인터 사살", "free()"),
            new DeathNote("THRD-DB07", "DeadLock-Alpha-4", "폭주 쿼리 긴급제압", "Ctrl+C"),
            new DeathNote("THRD-0128", "DeadLock-Alpha-5", "스택오버플로 사살", "SIGSEGV"),
            new DeathNote("THRD-P003", "DeadLock-Alpha-6", "방치된 커넥션 처단", "imteout"),
            new DeathNote("THRD-C999", "DeadLock-Alpha-7", "폭주 스케줄러 격리", "systemctl stop"),
            new DeathNote("THRD-W001", "DeadLock-Alpha-8", "영생 루프 종결", "Ctrl+z")
        );
        return new ListItemReader<>(victims);
    }

//    @Bean
//    @StepScope
//    public FlatFileItemWriter<DeathNote> deathNoteWriter(@Value("#{jobParameters['outputPath']}") String outputPath) {
//        return new FlatFileItemWriterBuilder<DeathNote>()
//            .name("deathNoteWriter")
//            .resource(new FileSystemResource(outputPath + "/death_notes.csv"))
//            .delimited()
//            .delimiter(",")
//            .sourceType(DeathNote.class)
//            .names("threadId", "victimName", "description", "executionMethod")
//            .headerCallback(writer -> writer.write("스레드ID,처형대상,처형사유,처형방식"))
//            .build();
//    }

//    @Bean
//    @StepScope
//    public FlatFileItemWriter<DeathNote> deathNoteWriter(@Value("#{jobParameters['outputPath']}") String outputPath) {
//        return new FlatFileItemWriterBuilder<DeathNote>()
//            .name("deathNoteWriter")
//            .resource(new FileSystemResource(outputPath + "/death_notes.csv"))
//            .delimited()
//            .delimiter(",")
//            .fieldExtractor(item -> new Object[] {
//                item.getTreadId() + ":" + item.getVictimName(),
//                item.getExecutionMethod(),
//                item.getDescription()
//            })
//            .headerCallback(writer -> writer.write("처형대상 식별정보,처형방식,처형사유"))
//            .build();
//    }

//    @Bean
//    @StepScope
//    public FlatFileItemWriter<DeathNote> deathNoteWriter(@Value("#{jobParameters['outputPath']}") String outputPath) {
//        return new FlatFileItemWriterBuilder<DeathNote>()
//            .name("deathNoteWriter")
//            .resource(new FileSystemResource(outputPath + "/death_notes_report.txt"))
//            .formatted()
//            .format("스레드 ID: %s | 처형대상: %s | 처형사유: %s | 처형방식: %s")
//            .sourceType(DeathNote.class)
//            .names("traceId", "victimName", "description", "executionMethod")
//            .headerCallback(writer -> writer.write("============= 처형 기록부 ============="))
//            .footerCallback(writer -> writer.write("============= 처형 완료 ============="))
//            .build();
//    }

    @Bean
    @StepScope
    public FlatFileItemWriter<DeathNote> deathNoteWriter() {
        return new FlatFileItemWriterBuilder<DeathNote>()
            .name("deathNoteWriter")
            .formatted()
            .format("스레드 ID: %s | 처형대상: %s | 처형사유: %s | 처형방식: %s")
            .sourceType(DeathNote.class)
            .names("traceId", "victimName", "description", "executionMethod")
            .headerCallback(writer -> writer.write("============= 처형 기록부 ============="))
            .footerCallback(writer -> writer.write("============= 처형 완료 ============="))
            .build();
    }

    @Bean
    @StepScope
    public MultiResourceItemWriter<DeathNote> multiResourceDeathNoteWriter(@Value("#{jobParameters['outputPath']}") String outputPath) {
        return new MultiResourceItemWriterBuilder<DeathNote>()
            .name("multiResourceDeathNoteWriter")
            .resource(new FileSystemResource(outputPath + "/death_notes_report.txt"))
            .itemCountLimitPerResource(5)
            .delegate(deathNoteWriter())
            .resourceSuffixCreator(index -> String.format("_%03d.txt", index))
            .build();
    }

    @AllArgsConstructor
    @Data
    public static class DeathNote {
        private String treadId;
        private String victimName;
        private String description;
        private String executionMethod;
    }


}
