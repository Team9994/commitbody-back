package team9499.commitbody.domain.routin.dto.rqeust;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.routin.dto.RoutineSetsDto;

import java.util.List;

@Schema(name = "루틴 편집 Request")
@Data
public class UpdateRoutineRequest {

    @Schema(description = "업데이트할 루틴명 (선택)")
    private String updateRoutineName;

    @Schema(description = "삭제할 상세 루틴 (선택)")
    private List<Long> deleteRoutines;

    @Schema(description = "수정할 세트 (선택)")
    private List<UpdateSets> updateSets;

    @Schema(description = "삭제할 세트 (선택)")
    private List<DeleteSets> deleteSets;

    @Schema(description = "추가할 운동 (선택),exerciseId, source, sets(총 세트 수) 를 사용해 운동 정보를 전달합니다. 세트수는 운동 별로 세트수 작성")
    private List<ExerciseDto> newExercises;

    @Schema(description = "대체할 운동 (선택)")
    private List<ChangeExercise> changeExercises;

    @Schema(description = "운동 순서 변경 (선택)")
    private List<ChangeOrders> changeOrders;

    
    @Schema(name = "세트 업데이트")
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateSets {
        @Schema(description = "상세 루틴 ID")
        private Long routineDetailsId;

        @Schema(description = "새로운 세트")
        private List<RoutineSetsDto> newSets;

        @Schema(description = "업데이트 세트")
        private List<RoutineSetsDto> updateSets;
    }

    @Schema(name = "세트 삭제")
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeleteSets {
        @Schema(description = "상세 루틴 ID")
        private Long routineDetailsId;

        @Schema(description = "세트 ID")
        private List<Long> setsIds;
    }
    @Schema(name = "운동 변경")
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChangeExercise{
        @Schema(description = "상세 루틴 ID")
        private Long routineDetailsId;

        @Schema(description = "운동 ID")
        private Long exerciseId;

        @Schema(description = "운동 타입[default,custom]")
        private String source;
    }

    @Schema(name = "운동 순서 변경")
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChangeOrders{
        @Schema(description = "상세 루틴 ID")
        private Long routineDetailsId;

        @Schema(description = "순서")
        private Integer orders;
    }
    
}