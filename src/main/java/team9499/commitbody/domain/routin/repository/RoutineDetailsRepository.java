package team9499.commitbody.domain.routin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.routin.domain.RoutineDetails;

@Repository
public interface RoutineDetailsRepository extends JpaRepository<RoutineDetails, Long> {
}
