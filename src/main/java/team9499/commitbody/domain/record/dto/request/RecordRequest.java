package team9499.commitbody.domain.record.dto.request;

import lombok.Data;
import team9499.commitbody.domain.record.dto.RecordDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecordRequest {

    private String recordName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private List<RecordDto> exercises;

}
