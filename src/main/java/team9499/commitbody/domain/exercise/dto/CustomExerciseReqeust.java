package team9499.commitbody.domain.exercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;

@Schema(name = "커스텀 운동 등록 Request")
@Data
public class CustomExerciseReqeust {

    @Schema(description = "커스텀 운동명")
    private String exerciseName;

    @Schema(description = "운동 장비")
    private ExerciseEquipment exerciseEquipment;

    @Schema(description = "운동 부위")
    private ExerciseTarget exerciseTarget;
}
