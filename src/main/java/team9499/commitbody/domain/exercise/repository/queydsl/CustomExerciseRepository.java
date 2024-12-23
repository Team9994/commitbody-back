package team9499.commitbody.domain.exercise.repository.queydsl;

import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.domain.exercise.dto.ReportDto;

public interface CustomExerciseRepository {

      ReportDto getWeeklyExerciseVolumeReport(Long memberId, Long exerciseId, ExerciseType exerciseType);

}
