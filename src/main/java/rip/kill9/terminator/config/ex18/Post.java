package rip.kill9.terminator.config.ex18;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
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
