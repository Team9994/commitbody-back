package team9499.commitbody.domain.exercise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.exercise.domain.ExerciseMethod;

import java.util.List;

@Repository
public interface ExerciseMethodRepository extends JpaRepository<ExerciseMethod, Long> {

    List<ExerciseMethod> findAllByExerciseId(Long exerciseId);
    
}
