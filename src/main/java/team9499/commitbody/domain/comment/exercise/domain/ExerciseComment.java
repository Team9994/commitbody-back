package team9499.commitbody.domain.comment.exercise.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.global.utils.BaseTime;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExerciseComment extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_comment_id")
    private Long id;

    @Column(length = 1000)
    private String content;

    @JoinColumn(name = "member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    private Integer likeCount;


    public static ExerciseComment of(Member member, String content){
        return ExerciseComment.builder().member(member).content(content).likeCount(0).build();
    }


}
