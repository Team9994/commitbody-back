package team9499.commitbody.domain.routin.dto.rqeust;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(name = "루린 등록 Request")
@Data
public class RoutineRequest {

    @Schema(description = "루틴 명")
    private String routineName;
    
    @Schema(description = "추가할 운동")
    private List<RoutineExercise> routineExercises;

}
