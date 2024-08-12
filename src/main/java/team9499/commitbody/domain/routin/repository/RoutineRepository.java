package team9499.commitbody.domain.routin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.routin.domain.Routine;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoutineRepository extends JpaRepository<Routine,Long> {
    List<Routine> findAllByMemberId(Long memberId);
    Optional<Routine> findByIdAndMemberId(Long routineId, Long memberId);
}
