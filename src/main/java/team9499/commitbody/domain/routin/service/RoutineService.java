package team9499.commitbody.domain.routin.service;

import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.routin.dto.response.MyRoutineResponse;
import team9499.commitbody.domain.routin.dto.rqeust.RoutineExercise;

import java.util.List;

import static team9499.commitbody.domain.routin.dto.rqeust.UpdateRoutineRequest.*;

public interface RoutineService {
    void saveRoutine(Long memberId, List<RoutineExercise> routineExercises, String routineName);

    MyRoutineResponse getMyRoutine(Long memberId);

    void updateRoutine(Long routineId, Long memberId, String routineName, List<Long> deleteRoutines, List<UpdateSets> updateSets,
                       List<DeleteSets> deleteSets, List<ExerciseDto> newExercises, List<ChangeExercise> changeExercises, List<ChangeOrders> changeOrders);

    void deleteRoutine(Long routineId,Long memberId);
}
