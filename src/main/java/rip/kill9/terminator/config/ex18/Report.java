package rip.kill9.terminator.config.ex18;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "reports")
public class Report {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    private String reportType;
    private int reporterLevel;
    private String evidenceData;
    private LocalDateTime reportedAt;

}