package team9499.commitbody.domain.record.repository;

import team9499.commitbody.domain.record.dto.response.RecordResponse;

public interface CustomRecordRepository {

    RecordResponse findByRecordId(Long recordId, Long memberId);
}
