package team9499.commitbody.domain.exercise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;

@Repository
public interface CustomExerciseRepository extends JpaRepository<CustomExercise, Long> {
}
