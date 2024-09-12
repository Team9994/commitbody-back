package team9499.commitbody.domain.like.service;

public interface LikeService {

    String updateCommentLike(Long exCommentId, Long memberId);

    String articleLike(Long articleId, Long memberId);

    String articleCommentLike(Long memberId,Long commentId);
}
