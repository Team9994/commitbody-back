package team9499.commitbody.domain.like.exercise.service;

public interface ExerciseCommentLikeService {

    String updateCommentLike(Long exCommentId, Long memberId);

    String articleLike(Long articleId, Long memberId);
}
