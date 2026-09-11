package rip.kill9.terminator.config.ex18;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
@NamedQuery(
    name = "Post.findByReportsReportedAtBetween",
    query = "SELECT p FROM Post p JOIN FETCH p.reports r WHERE r.reportedAt >= :startDateTime AND r.reportedAt < :endDateTime"
)
@Table(name = "posts")
@Entity
public class Post {

    @Id
    private Long id;
    private String title;
    private String content;
    private String writer;

    @OneToMany(mappedBy = "post")
    private List<Report> reports = new ArrayList<>();
}
