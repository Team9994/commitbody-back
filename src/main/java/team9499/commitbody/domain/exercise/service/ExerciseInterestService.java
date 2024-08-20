package team9499.commitbody.domain.exercise.service;

public interface ExerciseInterestService {

    String updateInterestStatus(Long exerciseId, Long memberId, String source);

    boolean checkInterestStatus(Long exerciseId, Long memberId);
}
