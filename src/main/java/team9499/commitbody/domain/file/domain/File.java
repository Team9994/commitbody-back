package team9499.commitbody.domain.file.domain;

import jakarta.persistence.*;
import lombok.*;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.global.utils.BaseTime;

@Entity
@Data
@Table(indexes = {
        @Index(name = "idx_file_article",columnList = "article_id")
})
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class File extends BaseTime {

    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    private String originName;

    private String storedName;

    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @JoinColumn(name = "article_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private Article article;

    public static File of(String originName, String storedName, FileType fileType,Article article){
        return File.builder().originName(originName).storedName(storedName).fileType(fileType).article(article).build();
    }

    public void update(String originName, String storedName,FileType fileType){
        this.originName = originName;
        this.storedName = storedName;
        this.fileType = fileType;
    }
}
