package team9499.commitbody.domain.record.dto.request;

import lombok.Data;
import team9499.commitbody.domain.record.dto.RecordDto;

import java.util.List;

@Data
public class UpdateRecordRequest {

    private String recordName;

    private List<RecordDto> recordDtoList;
}
