package team9499.commitbody.domain.record.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.record.dto.RecordSetsDto;

import java.util.List;


@Data
@Schema(description = "기록 수정 Request")
public class UpdateRecordRequest {

    @Schema(description = "변경할 세트가 존재시 작성")
    private List<RecordUpdateSets> updateSets;      // 세트수 추가

    @Schema(description = "추가할 운동 존재시 작성, [exerciseId,orders,source]필드만 사용 합니다.")
    private List<ExerciseDto> newExercises;     // 운동 추가

    @Schema(description = "삭제할 세트 존재시 setsId 작성")
    private List<Long> deleteSetIds;      // 세트수 삭제

    @Schema(description = "변경할 순서 존재시 detailsId와 변경할 순서 작성")
    private List<ChangeOrders> changeOrders;        // 운동 순서 변경

    @Schema(description = "삭제할 운동 존재시 detailsId 작성")
    private List<Long> deleteDetailsIds;     // 운동 삭제


    @Schema(description = "기록 세트 업데이트")
    @Data
    public static class RecordUpdateSets {
        @Schema(description = "상세 루틴 ID")
        private Long recordDetailsId;

        @Schema(description = "새로운 세트")
        private List<RecordSetsDto> newSets;

        @Schema(description = "업데이트 세트")
        private List<RecordSetsDto> updateSets;
    }

    @Schema(description = "기록 순서 변경")
    @Data
    public static class ChangeOrders{

        private Long recordDetailsId;

        private Integer orders;
    }
    
}
