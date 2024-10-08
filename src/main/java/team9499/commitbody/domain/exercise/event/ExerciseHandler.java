package team9499.commitbody.domain.exercise.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import team9499.commitbody.domain.exercise.service.ElasticExerciseService;

@Component
@RequiredArgsConstructor
public class ExerciseHandler {

    private final ElasticExerciseService exerciseService;

    @EventListener
    public void ElSaveExercise(ElasticSaveExerciseEvent elasticSaveExerciseEvent){
       exerciseService.saveExercise(elasticSaveExerciseEvent.getCustomExerciseDto());
    }

    @EventListener
    public void ElUpdateExercise(ElasticUpdateExerciseEvent elasticUpdateExerciseEvent){
        exerciseService.updateExercise(elasticUpdateExerciseEvent.getCustomExerciseDto(), elasticUpdateExerciseEvent.getSource());
    }

    @EventListener
    public void ElDeleteExercise(ElasticDeleteExerciseEvent elasticDeleteExerciseEvent){
        exerciseService.deleteExercise(elasticDeleteExerciseEvent.getCustomExerciseId(),elasticDeleteExerciseEvent.getMemberId());
    }

    @EventListener
    public void ElInterestExercise(ElasticExerciseInterest exerciseInterest){
        exerciseService.changeInterest(exerciseInterest.getExerciseId(), exerciseInterest.getSource(),exerciseInterest.getStatus(),exerciseInterest.getMemberId());
    }
}
