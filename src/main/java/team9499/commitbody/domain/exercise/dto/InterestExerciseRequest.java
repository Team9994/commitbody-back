package team9499.commitbody.domain.exercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(name = "관심 운동 등록 해제 Request")
@Data
public class InterestExerciseRequest {

    @Schema(description = "운동 Id")
    private Long exerciseId;

    @Schema(description = "운동 제공 정보 [기본 운동 : default_ , 커스텀 운동 : custom_]")
    private String source;
}
