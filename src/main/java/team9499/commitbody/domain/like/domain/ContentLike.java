package team9499.commitbody.domain.like.domain;

import jakarta.persistence.*;
import lombok.*;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.comment.article.domain.ArticleComment;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"articleComment,exercise_comment_id"})
public class ContentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long id;

    private boolean likeStatus;     // 좋아요 상태

    @JoinColumn(name = "member_id",foreignKey = @ForeignKey (ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @JoinColumn(name = "exercise_comment_id",foreignKey = @ForeignKey (ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private ExerciseComment exerciseComment;

    @JoinColumn(name = "article_comment_id", foreignKey = @ForeignKey (ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private ArticleComment articleComment;      // 게시글 댓글

    @JoinColumn(name = "article_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private Article article;            // 게시글

    public static ContentLike createLike(Member member, ExerciseComment exerciseComment){
        return ContentLike.builder().member(member).exerciseComment(exerciseComment).likeStatus(false).build();
    }

    public static ContentLike createArticleLike(Member member, Article article){
        return ContentLike.builder().member(member).article(article).likeStatus(true).build();
    }

    public static ContentLike creatArticleCommentLike(Member member, ArticleComment articleComment){
        return ContentLike.builder().member(member).articleComment(articleComment).article(articleComment.getArticle()).likeStatus(true).build();
    }
    public void changeLike(boolean likeStatus){
        this.likeStatus = likeStatus;
    }

}
