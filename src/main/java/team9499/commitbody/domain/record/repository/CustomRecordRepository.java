package team9499.commitbody.domain.record.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import team9499.commitbody.domain.record.dto.response.RecordMonthResponse;
import team9499.commitbody.domain.record.dto.response.RecordResponse;

import java.time.LocalDateTime;
import java.util.Map;

import static team9499.commitbody.domain.record.dto.response.RecordMonthResponse.*;

public interface CustomRecordRepository {

    RecordResponse findByRecordId(Long recordId, Long memberId);
    void deleteCustomExercise(Long customExerciseId);
    void deleteRecord(Long recordId, Long memberId);
    Map<String, RecordData> getRecordCountAdnDataForMonth(Long memberId);
    Slice<RecordDay> getRecordPage(Long memberId, LocalDateTime lastTime, Pageable pageable);
}
