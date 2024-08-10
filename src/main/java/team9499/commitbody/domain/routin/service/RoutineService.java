package team9499.commitbody.domain.routin.service;

import java.util.List;

public interface RoutineService {
    void saveRoutine(Long memberId, List<Long> exerciseIds, List<Long> customExerciseIds, String routineName);
}
