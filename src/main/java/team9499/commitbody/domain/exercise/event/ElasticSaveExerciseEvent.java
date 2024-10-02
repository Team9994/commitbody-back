package team9499.commitbody.domain.exercise.event;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.exercise.dto.CustomExerciseDto;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ElasticSaveExerciseEvent {

    private CustomExerciseDto customExerciseDto;

}
