package team9499.commitbody.domain.exercise.service;

public interface ElasticExerciseService {

    void saveExercise(Long customExerciseId);

    void updateExercise(Long customExerciseId,String source);

    void deleteExercise(Long customExerciseId);
}
