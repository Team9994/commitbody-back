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

        try {
            // content_like에서 삭제할 ID 목록 준비
            String likeDelete = "DELETE FROM content_like WHERE article_comment_id = ?";
            List<Object[]> likeBatchArgs = ids.stream()
                    .map(id -> new Object[]{id})
                    .toList();

            // article_comment에서 댓글 삭제할 ID 목록 준비
            String parentDelete = "DELETE FROM article_comment WHERE article_comment_id = ?";
            Object[] parentArgs = {commentId};

            // article_comment에서 자식 댓글 삭제할 ID 목록 준비
            String childDelete = "DELETE FROM article_comment WHERE parent_id = ?";
            Object[] childArgs = {commentId};

            // 부모 댓글 삭제시 해당 댓글로 날라간 알림 내역 삭제
            String notification = "DELETE FROM notification where comment_id = ?";
            List<Object[]> notificationArgs = ids.stream()
                    .map(id -> new Object[]{id})
                    .toList();

            // 각 테이블에서 배치 삭제 수행
            jdbcTemplate.batchUpdate(likeDelete, likeBatchArgs);
            jdbcTemplate.batchUpdate(notification, notificationArgs);
            jdbcTemplate.update(parentDelete, parentArgs);
            jdbcTemplate.update(childDelete, childArgs);
        }catch (Exception e){
            log.error("부모 댓글 배치 삭제시 오류 발생");
            e.printStackTrace();
        }
    }

    /**
     * 자식댓글 삭제시 사용
     * 자식 댓글 삭제시 자식댓글의 좋아요와 해당 댓글의 알림기록을 모두 삭제합니다.
     * @param commentId 삭제할 댓글 ID
     */
    @Override
    public void deleteChildCommentBatch(Long commentId) {
        try {
            String delChildCommentLike = "DELETE FROM content_like WHERE article_comment_id = ?";
            String delNotification = "DELETE FROM notification WHERE comment_id = ?";
            String delCommentId = "DELETE FROM article_comment WHERE article_comment_id = ?";

            jdbcTemplate.update(delChildCommentLike, commentId);
            jdbcTemplate.update(delNotification, commentId);
            jdbcTemplate.update(delCommentId, commentId); 
        }catch (Exception e){
            log.error("자식 댓글 삭제중 오류 발생");
            e.printStackTrace();
        }
        
    }
}
