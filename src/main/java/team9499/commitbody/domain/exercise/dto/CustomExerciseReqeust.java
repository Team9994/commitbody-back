package team9499.commitbody.domain.exercise.dto;

import lombok.Data;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;

@Data
public class CustomExerciseReqeust {

    private String exerciseName;

    private ExerciseEquipment exerciseEquipment;

    private ExerciseTarget exerciseTarget;
}
