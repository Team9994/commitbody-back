package team9499.commitbody.domain.record.domain;

import jakarta.persistence.*;
import lombok.*;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "record")
public class RecordDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_details_id")
    private Long id;

    private Integer detailsSets;        // 기록별 총 세트수

    private Integer detailsReps;        // 기록별 총 횟수

    private Integer detailsVolume;      // 기록별 총 볼륨

    private Integer max1RM;              // 최대1RM
    
    private Integer maxReps;            // 최대 세트수

    private Integer sumTimes;       // 기록별 총 수행 시간

    private Integer orders;         // 운동 기록 순서

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_ex_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private CustomExercise customExercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Record record;

    @OneToMany(mappedBy = "recordDetails",cascade = CascadeType.ALL)
    private List<RecordSets> setsList = new ArrayList<>();

    public static RecordDetails create(Object exercise,Record record,Integer orders){
        RecordDetailsBuilder builder = RecordDetails.builder().orders(orders);
        if (exercise instanceof Exercise){
            builder.exercise((Exercise) exercise);
        }else
            builder.customExercise((CustomExercise) exercise);

        return builder.record(record).build();
    }
    public void updateDetailsReps(Integer reps){
        this.detailsReps = reps;
    }

    public void updateMaxReps(Integer reps){
        this.maxReps = reps;
    }

    public void updateMax1RM(Integer max1Rrm){
        this.max1RM = max1Rrm;
    }
    public void updateDetailsVolume(Integer detailsVolume){
        this.detailsVolume = detailsVolume;
    }

    public void updateDetailsTimes(Integer detailsTimes){
        this.sumTimes = detailsTimes;
    }

    public void updateOrder(Integer orders){
        this.orders = orders;
    }

    public void updateSets(Integer sets){
        this.detailsSets = sets;
    }

    public static RecordDetails of(Object exercise,Record record,Integer orders){
        RecordDetailsBuilder detailsBuilder = RecordDetails.builder().orders(orders).record(record).detailsSets(0);
        if (exercise instanceof Exercise){
            ExerciseType exerciseType = ((Exercise) exercise).getExerciseType();
            if (exerciseType.equals(ExerciseType.REPS_ONLY)){
                detailsBuilder.maxReps(0).detailsReps(0);
            }else if (exerciseType.equals(ExerciseType.TIME_ONLY)){
                detailsBuilder.sumTimes(0);
            }else{
                detailsBuilder.maxReps(0).detailsVolume(0).max1RM(0);
            }
            detailsBuilder.exercise((Exercise) exercise);
        }else{
            detailsBuilder.customExercise((CustomExercise) exercise).maxReps(0).max1RM(0);
        }
        return detailsBuilder.build();
    }

    public RecordDetails onlyExercise(Object exercise) {
        RecordDetailsBuilder builder = RecordDetails.builder();
        if (exercise instanceof Exercise)
            builder.exercise((Exercise) exercise);
        else
            builder.customExercise((CustomExercise) exercise);

        return builder.build();
    }
}
