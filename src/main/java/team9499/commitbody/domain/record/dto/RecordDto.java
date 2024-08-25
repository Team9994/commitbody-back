package team9499.commitbody.domain.record.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(name = "기록Dto")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordDto {

    private Long recordId;

    private LocalDateTime endTime;

    private String recordName;


    @Schema(description = "운동 ID")
    private Long exerciseId;

    @Schema(description = "운동 제공 타입 [default , custom]")
    private String source;
    
    @Schema(description = "운동별 세트 리스트")
    private List<RecordSetsDto> sets;
}
