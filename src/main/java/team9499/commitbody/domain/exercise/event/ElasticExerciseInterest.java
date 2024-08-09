package team9499.commitbody.domain.exercise.event;

import lombok.Data;

@Data
public class ElasticExerciseInterest {
    
    private Long exerciseId;
    
    private String source;

    private String status;

    public ElasticExerciseInterest(Long exerciseId, String source,String status) {
        this.exerciseId = exerciseId;
        this.source = source;
        this.status = status;
    }
}
