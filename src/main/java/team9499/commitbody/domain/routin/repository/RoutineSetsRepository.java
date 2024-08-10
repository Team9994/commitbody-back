package team9499.commitbody.domain.routin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.routin.domain.RoutineSets;

import java.util.List;

@Repository
public interface RoutineSetsRepository extends JpaRepository<RoutineSets, Long> {

    List<RoutineSets> findAllByRoutineDetailsId(Long routineDetailsId);
}
