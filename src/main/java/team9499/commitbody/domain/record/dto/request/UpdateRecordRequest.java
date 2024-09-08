package team9499.commitbody.domain.record.dto.request;

import jakarta.validation.Valid;
import lombok.Data;
import team9499.commitbody.domain.record.dto.RecordDto;

import java.util.List;

@Data
public class UpdateV2RecordRequest {

    private String recordName;

    @Valid
    private List<RecordDto> recordDtoList;
}
