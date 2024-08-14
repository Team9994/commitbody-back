package team9499.commitbody.domain.record.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.routin.domain.RoutineDetails;
import team9499.commitbody.domain.routin.domain.RoutineSets;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecordSets {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_sets")
    private Long id;

    private Integer weight;

    private Integer times;

    private Integer reps;

    @ManyToOne
    @JoinColumn(name = "record_details_id")
    private RecordDetails recordDetails;

    public static RecordSets ofWeightAndSets(Integer weight, Integer reps, RecordDetails recordDetails) { // 무게와 세트를 기록
        return RecordSets.builder().weight(weight).reps(reps).recordDetails(recordDetails).build();
    }

    public static RecordSets ofSets(Integer reps, RecordDetails recordDetails) { // 세트수만 기록
        return RecordSets.builder().reps(reps).recordDetails(recordDetails).build();
    }

    public static RecordSets ofTimes(Integer times,Integer reps, RecordDetails recordDetails) { // 시간만 기록
        return RecordSets.builder().times(times).reps(reps).recordDetails(recordDetails).build();
    }
}
