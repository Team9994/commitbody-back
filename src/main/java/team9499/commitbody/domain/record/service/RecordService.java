package team9499.commitbody.domain.record.service;

import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.record.dto.RecordDto;
import team9499.commitbody.domain.record.dto.response.RecordResponse;

import java.time.LocalDateTime;
import java.util.List;

import static team9499.commitbody.domain.record.dto.request.UpdateRecordRequest.*;

public interface RecordService {

    Long saveRecord(Long memberId , String recordName, LocalDateTime startTime, LocalDateTime endTime, List<RecordDto> recordDtos);

    RecordResponse getRecord(Long recordId,Long memberId);

    void updateRecord(Long memberId, Long recordId, List<RecordUpdateSets> updateSets, List<ExerciseDto> newExercises, List<Long> deleteSets, List<Long> deleteExercises, List<ChangeOrders> changeOrders);
}
