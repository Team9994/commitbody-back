package team9499.commitbody.domain.routin.service;

import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.routin.dto.response.MyRoutineResponse;
import team9499.commitbody.domain.routin.dto.rqeust.RoutineExercise;

import java.util.List;

public interface RoutineService {
    void saveRoutine(Long memberId, List<RoutineExercise> routineExercises, String routineName);

    MyRoutineResponse getMyRoutine(Long memberId);

    MyRoutineResponse getDetailMyRoutine(Long memberId, Long routineId);

    void updateRoutine(Long routineId, Long memberId, String routineName, List<ExerciseDto> exerciseDtos);
    void deleteRoutine(Long routineId,Long memberId);
}
