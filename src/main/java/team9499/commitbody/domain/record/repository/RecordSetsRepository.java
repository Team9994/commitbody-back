package team9499.commitbody.domain.record.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.record.domain.RecordSets;

@Repository
public interface RecordSetsRepository extends JpaRepository<RecordSets, Long> {
    void deleteById(Long setsId);
}
