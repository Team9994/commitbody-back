package team9499.commitbody.domain.exercise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.exercise.domain.ExerciseInterest;

import java.util.Optional;

@Repository
public interface ExerciseInterestRepository extends JpaRepository<ExerciseInterest, Long>{

    Optional<ExerciseInterest> findByExerciseIdAndMemberId(Long exerciseId, Long memberId);

    Optional<ExerciseInterest> findByCustomExerciseIdAndMemberId(Long customExerciseId,Long memberId);

    void deleteAllByCustomExerciseId(Long customExerciseId);
}
