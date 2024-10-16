package team9499.commitbody.domain.like.service;

import team9499.commitbody.domain.article.dto.response.ArticleCountResponse;

import java.util.List;

public interface LikeService {

    String exerciseCommentLike(Long exCommentId, Long memberId);

    ArticleCountResponse articleLike(Long articleId, Long memberId);

    String articleCommentLike(Long memberId,Long commentId);

    List<Long> getWriteDrawArticleIds(Long memberId);
}
