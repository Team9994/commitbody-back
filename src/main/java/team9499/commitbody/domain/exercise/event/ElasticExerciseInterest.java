package team9499.commitbody.domain.exercise.event;

import lombok.Data;

@Data
public class ElasticExerciseInterest {
    
    private Long exerciseId;
    
    private String source;

    private String status;

    private Long memberId;

    public ElasticExerciseInterest(Long exerciseId, String source,String status,Long memberId) {
        this.exerciseId = exerciseId;
        this.source = source;
        this.status = status;
        this.memberId = memberId;
    }
}
