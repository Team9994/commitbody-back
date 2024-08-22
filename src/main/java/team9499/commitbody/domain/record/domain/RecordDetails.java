package team9499.commitbody.domain.record.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_ex_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private CustomExercise customExercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Record record;

    public static RecordDetails create(Object exercise,Record record){
        RecordDetailsBuilder builder = RecordDetails.builder();
        if (exercise instanceof Exercise){
            builder.exercise((Exercise) exercise);
        }else
            builder.customExercise((CustomExercise) exercise);

        return builder.record(record).build();
    }

}
