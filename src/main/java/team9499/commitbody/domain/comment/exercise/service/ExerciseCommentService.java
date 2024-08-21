package team9499.commitbody.domain.comment.exercise.service;

public interface ExerciseCommentService {

    void saveExerciseComment(Long memberId, Long exerciseId, String source,String comment);
}
