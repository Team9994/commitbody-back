package team9499.commitbody.domain.comment.article.domain;

import jakarta.persistence.*;
import lombok.*;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.global.utils.BaseTime;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Table(name = "article_comment", indexes = {
        @Index(name = "idx_article_comment_created_desc",columnList = "parent_id, created_at desc")
})
@ToString(exclude = {"childComments"})
public class ArticleComment extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_comment_id")
    private Long id;

    @Column(length = 4999)
    private String content;

    @Column(name = "like_count")
    private Integer likeCount;

    @JoinColumn(name = "article_id",foreignKey =  @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private Article article;

    @JoinColumn(name = "member_id",foreignKey =  @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;      // 작성자 정보

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "parent_id", foreignKey =  @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ArticleComment parent; // 부모 댓글, null이면 이 댓글은 대댓글이 아닌 최상위 댓글

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArticleComment> childComments = new ArrayList<>(); // 대댓글 리스트

    // 최상위 댓글 생성용 팩토리 메소드
    public static ArticleComment of(Article article, Member member, String content, ArticleComment parent) {
        return ArticleComment.builder().article(article).member(member).content(content).parent(parent).likeCount(0).build();
    }

    // 대댓글 추가 메소드
    public void addChildComment(ArticleComment child) {
        childComments.add(child);
        child.setParent(this);
    }

    public void updateLikeCount(Integer count){
        this.likeCount = count;
    }

    public void updateContent(String content){
        this.content = content;
    }
}
