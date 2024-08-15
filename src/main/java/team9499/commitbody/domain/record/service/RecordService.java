package team9499.commitbody.domain.record.service;

import team9499.commitbody.domain.record.dto.RecordDto;
import team9499.commitbody.domain.record.dto.response.RecordResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface RecordService {

    void saveRecord(Long memberId , String recordName, LocalDateTime startTime, LocalDateTime endTime, List<RecordDto> recordDtos);

    RecordResponse getRecord(Long recordId,Long memberId);
}
