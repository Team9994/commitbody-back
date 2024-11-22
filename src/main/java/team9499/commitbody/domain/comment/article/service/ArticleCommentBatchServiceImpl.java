package team9499.commitbody.domain.comment.article.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class ArticleCommentBatchServiceImpl implements ArticleCommentBatchService {

    private static final String SQL_LIKE_DELETE = "DELETE FROM content_like WHERE article_comment_id = ?";
    private static final String SQL_PARENT_DELETE = "DELETE FROM article_comment WHERE article_comment_id = ?";
    private static final String SQL_CHILD_DELETE = "DELETE FROM article_comment WHERE parent_id = ?";
    private static final String SQL_NOTIFICATION_DELETE = "DELETE FROM notification WHERE comment_id = ?";

    @Qualifier("dataJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    /**
     * 부모 댓글 삭제시 배치를 통한 데이터 삭제
     * 대량의 데이터가 삭제될수 있기때문에 JPA를 사용하면 효울이 좋지않게 나올 가능성이 높아 JDBC를 통한 배치를 사용하여 대량의 데이터를 삭제하여 성능을 향상
     * @param commentId 삭제할 부모 댓글 ID
     * @param ids 삭제할 자식 댓글 리스트
     */
    @Override
    public void deleteCommentBatch(Long commentId, List<Long> ids) {
        handleBatchDelete(ids,commentId,true);
    }

    /**
     * 자식댓글 삭제시 사용
     * 자식 댓글 삭제시 자식댓글의 좋아요와 해당 댓글의 알림기록을 모두 삭제합니다.
     * @param commentId 삭제할 댓글 ID
     */
    @Override
    public void deleteChildCommentBatch(Long commentId) {
        handleBatchDelete(List.of(commentId),commentId,false);
    }

    private void handleBatchDelete(List<Long> ids, Long parentId, boolean isParent) {
        try {
            jdbcTemplate.batchUpdate(SQL_LIKE_DELETE, mapToBatchArgs(ids));
            jdbcTemplate.batchUpdate(SQL_NOTIFICATION_DELETE, mapToBatchArgs(ids));

            if (isParent){
                jdbcTemplate.update(SQL_PARENT_DELETE, parentId);
                jdbcTemplate.update(SQL_CHILD_DELETE, parentId);
                return;
            }
            jdbcTemplate.update(SQL_CHILD_DELETE, parentId);

        }catch (Exception e){
            log.error("댓글 삭제 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private static List<Object[]> mapToBatchArgs(List<Long> ids) {
        return ids.stream()
                .map(id -> new Object[]{id})
                .toList();
    }
}
