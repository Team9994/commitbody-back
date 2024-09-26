package team9499.commitbody.domain.exercise.domain;

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
@Table(name = "exercise_interest")
public class ExerciseInterest extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_interest_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_ex_id")
    private CustomExercise customExercise;

    @Column(name = "is_interested")
    boolean isInterested;       // true : 관심 운동 , false : 비관심 운동

    public static ExerciseInterest exerciseInterest(Member member,Exercise exercise){
        return ExerciseInterest.builder().member(member).exercise(exercise).isInterested(false).build();
    }
    public static ExerciseInterest exerciseInterest(Member member,CustomExercise customExercise){
        return ExerciseInterest.builder().member(member).customExercise(customExercise).isInterested(false).build();
    }

    public void changeInterested(boolean isInterested){
        this.isInterested = isInterested;
    }
}
