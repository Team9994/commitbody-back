package team9499.commitbody.domain.comment.article.service;

import java.util.List;

public interface ArticleCommentBatchService {

    void deleteCommentBatch(Long commentId, List<Long> ids);

    void deleteChildCommentBatch(Long commentId);
}
