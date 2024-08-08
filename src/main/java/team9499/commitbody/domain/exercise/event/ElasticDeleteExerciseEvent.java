package team9499.commitbody.domain.exercise.event;

import lombok.Data;

@Data
public class ElasticDeleteExerciseEvent {

    private Long customExerciseId;

    public ElasticDeleteExerciseEvent(Long customExerciseId) {
        this.customExerciseId = customExerciseId;
    }
}
