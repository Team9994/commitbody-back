package team9499.commitbody.domain.comment.exercise.domain;

import jakarta.persistence.*;
import lombok.*;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.like.domain.ContentLike;
import team9499.commitbody.global.utils.BaseTime;

import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "exercise_comment", indexes = {
        @Index(name = "exercise_id_created_at_idx", columnList = "exercise_id, created_at DESC"),
        @Index(name = "custom_ex_id_created_at_idx", columnList = "custom_ex_id, created_at DESC"),
        @Index(name = "member_id_idx", columnList = "member_id")
})
@ToString(exclude = "exerciseCommentLikes")
public class ExerciseComment extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_comment_id")
    private Long id;

    @Column(length = 1000)
    private String content;

    @JoinColumn(name = "member_id",foreignKey = @ForeignKey (ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @JoinColumn(name = "exercise_id",foreignKey = @ForeignKey (ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private Exercise exercise;

    @JoinColumn(name = "custom_ex_id",foreignKey = @ForeignKey (ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    private CustomExercise customExercise;

    @OneToMany(mappedBy = "exerciseComment",cascade = CascadeType.REMOVE)
    private List<ContentLike> exerciseCommentLikes;

    @Column(name = "like_count")
    private Integer likeCount;      // 좋아요수

    @Column(name = "like_status")
    private boolean likeStatus;     // 좋아요 상태

    public static ExerciseComment of(Member member,Object exercise ,String content){
        ExerciseCommentBuilder exerciseCommentBuilder = ExerciseComment.builder().member(member).content(content).likeCount(0).likeStatus(false);
        if (exercise instanceof Exercise){
            exerciseCommentBuilder.exercise((Exercise) exercise);
        }else
            exerciseCommentBuilder.customExercise((CustomExercise) exercise);

        return exerciseCommentBuilder.build();
    }

    public void updateLikeCount(Integer likeCount){
       this.likeCount = likeCount;
    }

    public void updateContent(String newContent){
        this.content = newContent;
    }

}
