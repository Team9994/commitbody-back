package team9499.commitbody.domain.record.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "기록 운동 세트 Dto")
public class RecordSetsDto {

    private Long setsId;

    @Schema(description = "무게")
    private Integer weight;

    @Schema(description = "소요 시간")
    private Integer times;

    @Schema(description = "수행 횟수")
    private Integer reps;
}
