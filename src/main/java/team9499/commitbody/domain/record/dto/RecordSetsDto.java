package team9499.commitbody.domain.record.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.record.domain.Record;
import team9499.commitbody.domain.record.domain.RecordSets;
import team9499.commitbody.domain.record.dto.response.RecordSetsResponse;

import java.time.LocalDate;
import java.util.List;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    private LocalDate localDateTime;

    private List<RecordSetsResponse> recordSetsResponseList;

    public static RecordSetsDto of(Record record, RecordSets recordSets){
        return RecordSetsDto.builder().localDateTime(record.getStartTime().toLocalDate())
                .reps(recordSets.getReps())
                .times(recordSets.getTimes())
                .weight(recordSets.getWeight())
                .build();
    }
}
