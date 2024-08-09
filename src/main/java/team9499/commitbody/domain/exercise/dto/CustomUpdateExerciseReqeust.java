package team9499.commitbody.domain.exercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.global.annotations.ValidEnum;

@Schema(name = "커스텀 운동 수정 Request")
@Data
public class CustomUpdateExerciseReqeust {

    private Long customExerciseId;
    
    @Schema(description = "커스텀 운동명")
    @NotBlank(message = "운동명을 입력해주세요")
        private String exerciseName;

    @Schema(description = "운동 장비")
    @ValidEnum(message = "운동 장비를 입력해주세요" ,enumClass = ExerciseEquipment.class)
    private ExerciseEquipment exerciseEquipment;

    @Schema(description = "운동 부위")
    @ValidEnum(message = "운동 부위를 입력해주세요",enumClass = ExerciseTarget.class)
    private ExerciseTarget exerciseTarget;

    @Schema(description = "운동 등록타입 전송시 ' custom_ / default_ ' 보냅니다.")
    @NotBlank(message = "운동 등록 타입을 선택해주세요")
    private String source;

}
