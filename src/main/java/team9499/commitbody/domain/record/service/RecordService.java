package team9499.commitbody.domain.record.service;

import org.springframework.data.domain.Pageable;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.record.dto.RecordDto;
import team9499.commitbody.domain.record.dto.response.RecordMonthResponse;
import team9499.commitbody.domain.record.dto.response.RecordResponse;

import java.time.LocalDateTime;
import java.util.List;

import static team9499.commitbody.domain.record.dto.request.UpdateRecordRequest.*;

public interface RecordService {

    Long saveRecord(Long memberId , String recordName, LocalDateTime startTime, LocalDateTime endTime, List<RecordDto> recordDtos);

    RecordResponse getRecord(Long recordId,Long memberId);

    void updateRecord(Long memberId, Long recordId, String recordName , List<RecordDto> recordDtos);

    void deleteRecord(Long memberId, Long recordId);

    RecordMonthResponse getRecordForMember(Long memberId,LocalDateTime lastTime, Pageable pageable);
}
