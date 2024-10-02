package team9499.commitbody.domain.exercise.service;

import team9499.commitbody.domain.exercise.dto.CustomExerciseDto;

public interface ElasticExerciseService {

    void saveExercise(CustomExerciseDto customExerciseDto);

    void updateExercise(CustomExerciseDto customExerciseDto,String source);

    void deleteExercise(Long customExerciseId,Long memberId);

    void changeInterest(Long exerciseId,String source,String status,Long memberId);

    void updateExerciseInterestWithDrawAsync(Long memberId , boolean type);
}
