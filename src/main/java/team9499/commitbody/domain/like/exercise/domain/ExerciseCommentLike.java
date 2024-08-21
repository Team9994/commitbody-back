package team9499.commitbody.domain.like.exercise.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExerciseCommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ex_comment_like_id")
    private Long id;

    private boolean likeStatus;     // 좋아요 상태

    @JoinColumn(name = "member_id",foreignKey = @ForeignKey (ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @JoinColumn(name = "exercise_comment_id",foreignKey = @ForeignKey (ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private ExerciseComment exerciseComment;

    public static ExerciseCommentLike createLike(Member member, ExerciseComment exerciseComment){
        return ExerciseCommentLike.builder().member(member).exerciseComment(exerciseComment).likeStatus(false).build();
    }
    public void changeLike(boolean likeStatus){
        this.likeStatus = likeStatus;
    }

}
