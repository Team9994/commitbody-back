package team9499.commitbody.domain.exercise.event;


import lombok.Getter;

@Getter
public class ElasticUpdateExerciseEvent {

    private Long customExerciseId;
    private String source;

    public ElasticUpdateExerciseEvent(Long customExerciseId,String source) {
        this.customExerciseId = customExerciseId;
        this.source = source;
    }
}
