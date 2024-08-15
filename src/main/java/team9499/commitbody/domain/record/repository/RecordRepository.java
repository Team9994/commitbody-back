package team9499.commitbody.domain.record.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.record.domain.Record;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long>, CustomRecordRepository {
}
