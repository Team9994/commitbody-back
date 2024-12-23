package team9499.commitbody.domain.record.repository;

import team9499.commitbody.domain.record.dto.RecordSetsDto;
import team9499.commitbody.domain.record.dto.response.RecordResponse;
import team9499.commitbody.domain.record.dto.response.RecordSetsResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static team9499.commitbody.domain.record.dto.response.RecordMonthResponse.*;

public interface CustomRecordRepository {

    RecordResponse findByRecordId(Long recordId, Long memberId);

    void deleteCustomExercise(Long customExerciseId);

    void deleteRecord(Long recordId, Long memberId);

    Map<String, RecordData> getRecordCountAdnDataForMonth(Long memberId,Integer year, Integer month);

    List<RecordDay> getRecordPage(Long memberId, Integer year, Integer month);

    Map<LocalDate, List<RecordSetsResponse>> getRecentRecordsByExercise(Long exerciseId, Long memberId, String source);
}
