package team9499.commitbody.domain.routin.dto.rqeust;

import lombok.Data;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;

import java.util.List;

@Data
public class EditRoutineRequest {

    private String routineName;

    private List<ExerciseDto> exercises;
}
