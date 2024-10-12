package team9499.commitbody.domain.comment.article.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleCommentBatchServiceImplTest {


    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private ArticleCommentBatchServiceImpl articleCommentBatchService;

    @BeforeEach
    void init(){
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deleteCommentBatch_ShouldDeleteCommentsAndRelatedData() {
        // Given
        Long commentId = 1L;
        List<Long> childCommentIds = List.of(2L, 3L);

        List<Object[]> list = childCommentIds.stream()
                .map(id -> new Object[]{id})
                .toList();
        // When
        articleCommentBatchService.deleteCommentBatch(commentId, childCommentIds);

        // Then
        // Verify that batchUpdate was called with the correct arguments
        verify(jdbcTemplate, times(1)).batchUpdate(eq("DELETE FROM content_like WHERE article_comment_id = ?"), eq(list));
//        verify(jdbcTemplate, times(1)).batchUpdate(eq("DELETE FROM notification where comment_id = ?"), anyList());
//        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM article_comment WHERE article_comment_id = ?"), eq(new Object[]{commentId}));
//        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM article_comment WHERE parent_id = ?"), eq(new Object[]{commentId}));
    }

}