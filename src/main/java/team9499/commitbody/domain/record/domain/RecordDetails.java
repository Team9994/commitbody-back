package team9499.commitbody.domain.record.domain;

import jakarta.persistence.*;
import lombok.*;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.record.dto.RecordDetailsDto;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "record")
@Table(name = "record_details")
public class RecordDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_details_id")
    private Long id;

    @Column(name = "details_sets")
    private Integer detailsSets;        // 기록별 총 세트수

    @Column(name = "details_reps")
    private Integer detailsReps;        // 기록별 총 횟수

    @Column(name = "details_volume")
    private Integer detailsVolume;      // 기록별 총 볼륨

    private Integer max1RM;              // 최대1RM

    @Column(name = "max_reps")
    private Integer maxReps;            // 최대 세트수

    @Column(name = "sum_times")
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
        RecordDetailsBuilder builder = RecordDetails.builder().record(record).orders(orders);
        if (exercise instanceof Exercise){
            return builder.exercise((Exercise) exercise).build();
        }
        return builder.customExercise((CustomExercise) exercise).build();
    }

    public RecordDetails onlyExercise(Object exercise) {
        RecordDetailsBuilder builder = RecordDetails.builder();
        if (exercise instanceof Exercise)
            return builder.exercise((Exercise) exercise).build();
        return builder.customExercise((CustomExercise) exercise).build();
    }

    public void setWeight(RecordDetailsDto detailsDto){
        this.detailsVolume = detailsDto.getDetailsVolume();
        this.detailsReps = detailsDto.getDetailsReps();
        this.max1RM = detailsDto.getMaxRm()/detailsDto.getWeightCount();
    }
    public void setReps(RecordDetailsDto detailsDto){
        this.detailsSets = detailsDto.getDetailsSets();
        this.maxReps = detailsDto.getMaxReps();
        this.detailsReps = detailsDto.getDetailsReps();
    }
}
