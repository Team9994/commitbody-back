package team9499.commitbody.domain.routin.domain;

import jakarta.persistence.*;
import lombok.*;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;

import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "routine")
@Table(name = "routine_details")
public class RoutineDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routine_detail_id")
    private Long id;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "custom_ex_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private CustomExercise customExercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private Routine routine;

    @Column(name = "total_sets")
    private Integer totalSets;      // 총 세트수

    private Integer orders;         // 운동 순서

    public static RoutineDetails of(Object exercise, Routine routine,Integer orders){
        RoutineDetailsBuilder routineDetailsBuilder = RoutineDetails.builder().orders(orders).routine(routine);
        if (exercise instanceof Exercise){
            routineDetailsBuilder.exercise((Exercise) exercise).totalSets(4);
        }else{
            routineDetailsBuilder.customExercise((CustomExercise) exercise).totalSets(4).orders(orders);
        }
        return routineDetailsBuilder.build();

    }

    public static RoutineDetails of(ExerciseDto exerciseDto, Object exercise, Routine routine){
        RoutineDetailsBuilder routineDetailsBuilder = RoutineDetails.builder().id(exerciseDto.getRoutineDetailId()).routine(routine);
        if (exercise instanceof Exercise){
            return routineDetailsBuilder.exercise((Exercise) exercise).totalSets(4).build();
        }
        return routineDetailsBuilder.customExercise((CustomExercise) exercise).totalSets(4).orders(exerciseDto.getOrders()).build();
    }

    public static RoutineDetails of(Object exercise, Routine routine){
        RoutineDetailsBuilder routineDetailsBuilder = RoutineDetails.builder().routine(routine);
        if (exercise instanceof Exercise){
            routineDetailsBuilder.exercise((Exercise) exercise).totalSets(4);
        }else{
            routineDetailsBuilder.customExercise((CustomExercise) exercise).totalSets(4);
        }
        return routineDetailsBuilder.build();
    }

    public void updateOrders(Integer orders){
        this.orders = orders;
    }

    public void updateExercise(Object exercise){
        if (exercise instanceof Exercise){
            this.exercise = (Exercise) exercise;
        }
        if (exercise instanceof CustomExercise){
            this.customExercise = (CustomExercise) exercise;
        }
    }
}
