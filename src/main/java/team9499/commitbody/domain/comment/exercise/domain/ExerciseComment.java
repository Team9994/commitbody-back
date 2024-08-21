package team9499.commitbody.domain.comment.exercise.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.global.utils.BaseTime;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "exercise_id_created_at_idx", columnList = "exercise_id, created_at DESC"),
        @Index(name = "custom_ex_id_created_at_idx", columnList = "custom_ex_id, created_at DESC"),
        @Index(name = "member_id_idx", columnList = "member_id")
})
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

    private Integer likeCount;

    public static ExerciseComment of(Member member,Object exercise ,String content){
        ExerciseCommentBuilder exerciseCommentBuilder = ExerciseComment.builder().member(member).content(content).likeCount(0);
        if (exercise instanceof Exercise){
            exerciseCommentBuilder.exercise((Exercise) exercise);
        }else
            exerciseCommentBuilder.customExercise((CustomExercise) exercise);

        return exerciseCommentBuilder.build();
    }

}
