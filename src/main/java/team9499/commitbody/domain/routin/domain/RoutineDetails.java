package team9499.commitbody.domain.routin.domain;

import jakarta.persistence.*;
import lombok.*;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;

import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "routine")
public class RoutineDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routine_detail_id")
    private Long id;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "custom_ex_id")
    private CustomExercise customExercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private Routine routine;
    
    private Integer totalSets;      // 총 세트수

    @OneToMany(mappedBy = "routineDetails")
    private List<RoutineSets> detailsSets;

    public static RoutineDetails of(Object exercise, Routine routine){
        RoutineDetailsBuilder routineDetailsBuilder = RoutineDetails.builder().routine(routine);
        if (exercise instanceof Exercise){
            routineDetailsBuilder.exercise((Exercise) exercise).totalSets(calculateTotalSets((Exercise) exercise));
        }else{
            routineDetailsBuilder.customExercise((CustomExercise) exercise).totalSets(4);
        }
        return routineDetailsBuilder.build();

    }

    // 루틴 저장시 기본 세트수 저장
    private static Integer calculateTotalSets(Exercise exercise) {
        return switch (exercise.getExerciseType()) {
            case REPS_ONLY -> 4;
            case TIME_ONLY -> 1;
            default -> 5;
        };
    }

}
