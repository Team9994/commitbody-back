package team9499.commitbody.domain.record.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import team9499.commitbody.domain.record.dto.RecordDto;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "운동 기록 저장 Request")
@Data
public class RecordRequest {

    @Schema(description = "기록 이름")
    private String recordName;

    @Schema(description = "시작 시간")
    private LocalDateTime startTime;

    @Schema(description = "종료 시간")
    private LocalDateTime endTime;

    @Schema(description = "수행한 운동 리스트")
    private List<RecordDto> exercises;

}
