package team9499.commitbody.domain.like.service;

import team9499.commitbody.domain.article.dto.response.ArticleCountResponse;

public interface LikeService {

    String updateCommentLike(Long exCommentId, Long memberId);

    ArticleCountResponse articleLike(Long articleId, Long memberId);

    String articleCommentLike(Long memberId,Long commentId);
}
