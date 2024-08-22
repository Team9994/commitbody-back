package team9499.commitbody.domain.exercise.event;

import lombok.Data;

@Data
public class ElasticDeleteExerciseEvent {

    private Long customExerciseId;
    private Long memberId;

    public ElasticDeleteExerciseEvent(Long customExerciseId,Long memberId) {
        this.customExerciseId = customExerciseId;
        this.memberId = memberId;
    }
}
