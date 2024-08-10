package team9499.commitbody.domain.routin.service;

import team9499.commitbody.domain.routin.dto.response.MyRoutineResponse;

import java.util.List;

public interface RoutineService {
    void saveRoutine(Long memberId, List<Long> exerciseIds, List<Long> customExerciseIds, String routineName);

    MyRoutineResponse getMyRoutine(Long memberId);
}
