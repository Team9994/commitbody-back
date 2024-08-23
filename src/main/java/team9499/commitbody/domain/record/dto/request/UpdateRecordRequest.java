package team9499.commitbody.domain.record.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.routin.dto.RoutineSetsDto;

import java.util.List;


@Data
public class UpdateRecordRequest {

    private List<RecordUpdateSets> updateSets;      // 세트수 추가

    private List<ExerciseDto> newExercises;     // 운동 추가

    private List<Long> deleteSetIds;      // 세트수 삭제

    private List<ChangeOrders> changeOrders;        // 운동 순서 변경

    private List<Long> deleteDetailsIds;     // 운동 삭제


    @Data
    public static class RecordUpdateSets {
        @Schema(description = "상세 루틴 ID")
        private Long recordDetailsId;

        @Schema(description = "새로운 세트")
        private List<RoutineSetsDto> newSets;

        @Schema(description = "업데이트 세트")
        private List<RoutineSetsDto> updateSets;
    }

    @Data
    public static class ChangeOrders{

        private Long recordDetailsId;

        private Integer orders;
    }
    
}
