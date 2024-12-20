package team9499.commitbody.domain.exercise.service;

import team9499.commitbody.domain.exercise.dto.CustomExerciseDto;
import team9499.commitbody.domain.exercise.dto.SearchExerciseResponse;

public interface ElasticExerciseService {

    SearchExerciseResponse searchExercise(String name, String target, String equipment, Integer from, Integer size, Boolean like, String memberId, String exerciseType);

    void saveExercise(CustomExerciseDto customExerciseDto);

    void updateExercise(CustomExerciseDto customExerciseDto,String source);

    void deleteExercise(Long customExerciseId,Long memberId);

    void changeInterest(Long exerciseId,String source,String status,Long memberId);

    void updateExerciseInterestWithDrawAsync(Long memberId , boolean type);
}
