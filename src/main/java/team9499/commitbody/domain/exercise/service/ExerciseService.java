package team9499.commitbody.domain.exercise.service;

import team9499.commitbody.domain.exercise.dto.SearchExerciseResponse;

public interface ExerciseService {

    SearchExerciseResponse searchExercise(String name, String target,String equipment, Integer from, Integer size, Boolean like, String memberId);
}
