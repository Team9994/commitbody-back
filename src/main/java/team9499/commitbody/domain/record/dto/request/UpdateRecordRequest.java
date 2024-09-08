package team9499.commitbody.domain.record.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import team9499.commitbody.domain.record.dto.RecordDto;

import java.util.List;

@Data
@Schema(description = "기록 수정 Request")
public class UpdateRecordRequest {

    private String recordName;

    @Schema(description = "exerciseId, orders, source, sets[] 필드만 사용하면 됩니다.")
    private List<RecordDto> recordDtoList;
}
