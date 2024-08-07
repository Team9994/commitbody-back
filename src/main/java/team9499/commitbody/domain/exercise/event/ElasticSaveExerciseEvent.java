package team9499.commitbody.domain.exercise.event;


import lombok.Getter;

@Getter
public class ElasticSaveExerciseEvent {

    private Long customExerciseId;

    public ElasticSaveExerciseEvent(Long customExerciseId) {
        this.customExerciseId = customExerciseId;
    }
}
