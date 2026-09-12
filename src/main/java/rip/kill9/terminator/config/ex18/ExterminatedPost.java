package rip.kill9.terminator.config.ex18;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@Entity
@Table(name = "exterminated_posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExterminatedPost {

    @Id
    @Column(name = "post_id")
    private Long postId;
    private String writer;
    private String title;
    private int reportCount;
    private double score;
    private LocalDateTime exterminatedAt;

    @Builder
    public ExterminatedPost(Long postId, String writer, String title, int reportCount, double score,
                            LocalDateTime exterminatedAt) {
        this.postId = postId;
        this.writer = writer;
        this.title = title;
        this.reportCount = reportCount;
        this.score = score;
        this.exterminatedAt = exterminatedAt;
    }
}
