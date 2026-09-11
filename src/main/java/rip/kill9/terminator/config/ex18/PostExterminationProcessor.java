package rip.kill9.terminator.config.ex18;

import java.time.LocalDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class PostExterminationProcessor implements ItemProcessor<Post, ExterminatedPost> {

    @Override
    public @Nullable ExterminatedPost process(Post post) throws Exception {
        // 각 신고의 신뢰도를 기반으로 점수 계산
        double reportScore = calculateReportScore(post.getReports());
        return ExterminatedPost.builder()
            .postId(post.getId())
            .writer(post.getWriter())
            .title(post.getTitle())
            .reportCount(post.getReports().size())
            .score(reportScore)
            .exterminatedAt(LocalDateTime.now())
            .build();
    }

    private double calculateReportScore(List<Report> reports) {
        for (Report report : reports) {
            analyzeReportType(report.getReportType());
            checkReporterTrust(report.getReporterLevel());
            validateEvidence(report.getEvidenceData());
            calculateTimeValidity(report.getReportedAt());
        }
        return Math.random() * 10;
    }

    private void calculateTimeValidity(LocalDateTime reportedAt) {
    }

    private void analyzeReportType(String reportType) {
    }

    private void checkReporterTrust(int reportLevel) {
    }

    private void validateEvidence(String evidenceData) {
    }



}
