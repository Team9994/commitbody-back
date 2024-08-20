package team9499.commitbody.domain.exercise.repository.queydsl;

import team9499.commitbody.domain.exercise.dto.response.ExerciseResponse;

public interface CustomExerciseRepository {
      ExerciseResponse getExerciseDetailReport(Long memberId, Long exerciseId, String source);
}
