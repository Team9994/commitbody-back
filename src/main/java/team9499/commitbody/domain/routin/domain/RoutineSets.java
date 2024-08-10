package team9499.commitbody.domain.routin.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoutineSets {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sets_id")
    private Long id;

    private Integer kg;

    private Integer times;

    private Integer sets;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "routine_detail_id")
    private RoutineDetails routineDetails;

    public static RoutineSets ofWeightAndSets(Integer kg, Integer sets, RoutineDetails routineDetails) { // 무게와 세트를 기록
        return RoutineSets.builder().kg(kg).sets(sets).routineDetails(routineDetails).build();
    }

    public static RoutineSets ofSets(Integer sets, RoutineDetails routineDetails) { // 세트수만 기록
        return RoutineSets.builder().sets(sets).routineDetails(routineDetails).build();
    }

    public static RoutineSets ofTimes(Integer times, RoutineDetails routineDetails) { // 시간만 기록
        return RoutineSets.builder().times(times).routineDetails(routineDetails).build();
    }
}
