package rip.kill9.terminator.config.ex18;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class ExterminatedPost {
    private Long postId;
    private String writer;
    private String title;
    private int reportCount;
    private double score;
    private LocalDateTime exterminatedAt;
}
