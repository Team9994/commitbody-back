package team9499.commitbody.domain.exercise.event;


import lombok.Getter;

@Getter
public class ElasticUpdateExerciseEvent {

    private Long customExerciseId;

    public ElasticUpdateExerciseEvent(Long customExerciseId) {
        this.customExerciseId = customExerciseId;
    }
}
