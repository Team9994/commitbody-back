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
       exerciseService.saveExercise(elasticSaveExerciseEvent.getCustomExerciseId());
    }
}