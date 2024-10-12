package team9499.commitbody.domain.comment.article.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.dto.response.ArticleCountResponse;
import team9499.commitbody.domain.article.repository.ArticleRepository;
import team9499.commitbody.domain.comment.article.domain.ArticleComment;
import team9499.commitbody.domain.comment.article.domain.OrderType;
import team9499.commitbody.domain.comment.article.dto.ArticleCommentDto;
import team9499.commitbody.domain.comment.article.dto.response.ArticleCommentResponse;
import team9499.commitbody.domain.comment.article.repository.ArticleCommentRepository;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.notification.service.NotificationService;
import team9499.commitbody.global.redis.RedisService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static team9499.commitbody.domain.Member.domain.LoginType.*;
import static team9499.commitbody.domain.article.domain.ArticleCategory.*;
import static team9499.commitbody.domain.article.domain.ArticleType.*;
import static team9499.commitbody.domain.article.domain.Visibility.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ArticleCommentServiceTest {

    @Mock private ArticleRepository articleRepository;
    @Mock private ArticleCommentRepository articleCommentRepository;
    @Mock private ArticleCommentBatchService articleCommentBatchService;
    @Mock private RedisService redisService;
    @Mock private NotificationService notificationService;

    @InjectMocks private ArticleCommentServiceImpl articleCommentService;

    private Long memberId = 1L;
    private Long articleId = 2L;
    private Member member;
    private Member replyMember;
    private ArticleComment articleComment;
    private Article article;

    @BeforeEach
    void init(){
        member =  Member.builder().id(memberId).nickname("사용자").loginType(KAKAO).build();
        replyMember =  Member.builder().id(2L).nickname("답장 사용자").loginType(KAKAO).build();
        article = Article.builder().id(articleId).title("제목").content("내용").articleType(INFO_QUESTION).articleCategory(FEEDBACK).visibility(PUBLIC).member(member).commentCount(1).build();
        articleComment =  ArticleComment.builder().id(3L).article(article).member(member).content("대댓글").parent(null).childComments(new ArrayList<>()).build();
    }

    @DisplayName("댓글 저장")
    @Test
    void articleCommentSave(){
        when(articleRepository.findById(eq(articleId))).thenReturn(Optional.of(article));
        when(redisService.getMemberDto(memberId.toString())).thenReturn(Optional.of(member));
        when(articleCommentRepository.save(any())).thenReturn(articleComment);

        doNothing().when(notificationService).sendComment(any(),anyLong(),anyString(),anyString(),anyString(),anyLong());

        ArticleCountResponse response = articleCommentService.saveArticleComment(memberId, articleId, null, "대댓글", replyMember.getNickname());

        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getType()).isEqualTo("댓글 작성 성공");
        verify(notificationService, times(1)).sendComment(any(),anyLong(),anyString(),anyString(),anyString(),anyLong());
    }

    @DisplayName("대댓글 저장")
    @Test
    void replyCommentSave(){
        when(articleRepository.findById(eq(articleId))).thenReturn(Optional.of(article));
        when(redisService.getMemberDto(memberId.toString())).thenReturn(Optional.of(member));

        ArticleComment childComment = ArticleComment.builder().id(4L).article(article).member(member).content("대댓글").parent(articleComment).childComments(new ArrayList<>()).build();

        when(articleCommentRepository.getReferenceById(anyLong())).thenReturn(articleComment);

        when(articleCommentRepository.save(any())).thenReturn(childComment);
        doNothing().when(notificationService).sendReplyComment(any(),anyString(),anyString(),anyString(),anyString(),anyLong());

        ArticleCountResponse response = articleCommentService.saveArticleComment(memberId, articleId, articleComment.getId(), "대대댓글", replyMember.getNickname());

        assertThat(response.getType()).isEqualTo("대댓글 작성 성공");
        verify(notificationService,times(1)).sendReplyComment(any(),anyString(),anyString(),anyString(),anyString(),anyLong());
    }

    @DisplayName("댓글 수정")
    @Test
    void updateComment(){
        when(articleCommentRepository.findById(anyLong())).thenReturn(Optional.of(articleComment));
        doNothing().when(notificationService).updateNotification(anyLong(),anyString());

        articleCommentService.updateArticleComment(memberId,articleComment.getId(),"변경한 댓글");

        assertThat(articleComment.getContent()).isEqualTo("변경한 댓글");
        assertThat(articleComment.getContent()).isNotEqualTo("대댓글");
        verify(notificationService,times(1)).updateNotification(anyLong(),anyString());
    }

    @DisplayName("사용자가 아닌 타사용자가 접근시 403")
    @Test
    void otherMemberUseException(){
        when(articleCommentRepository.findById(anyLong())).thenReturn(Optional.of(articleComment));

        assertThatThrownBy(() -> articleCommentService.updateArticleComment(3L,articleComment.getId(),"타사용자 변경"))
                .isInstanceOf(InvalidUsageException.class)
                .hasMessage("작성자만 이용할 수 있습니다.");
    }
    
    @DisplayName("게시글의 작성된 댓글 모두 조회")
    @Test
    void getAllArticleComment(){

        List<ArticleCommentDto> articleCommentDtos = new ArrayList<>();
        articleCommentDtos.add(ArticleCommentDto.builder().commentId(1l).childComments(new ArrayList<>()).build());
        articleCommentDtos.add(ArticleCommentDto.builder().commentId(2l).childComments(new ArrayList<>()).build());
        articleCommentDtos.add(ArticleCommentDto.builder().commentId(3l).childComments(new ArrayList<>()).build());

        SliceImpl<ArticleCommentDto> commentDtoSlice = new SliceImpl<>(articleCommentDtos, Pageable.ofSize(10), false);
        when(articleCommentRepository.getAllCommentByArticle(anyLong(),anyLong(),isNull(),isNull(),eq(OrderType.RECENT),any(Pageable.class))).thenReturn(commentDtoSlice);
        when(articleCommentRepository.getCommentCount(anyLong(),anyLong())).thenReturn(3);

        ArticleCommentResponse comments = articleCommentService.getComments(articleId, memberId, null, null, OrderType.RECENT, Pageable.ofSize(10));

        assertThat(comments.isHasNext()).isFalse();
        assertThat(comments.getTotalCount()).isEqualTo(3);
        assertThat(comments.getComments().size()).isEqualTo(3);
    }
    
    @DisplayName("부모 댓글 삭제")
    @Test
    void deleteArticleComment(){
        List<Long> ids = List.of(4L,5L,6L);
        when(articleCommentRepository.findById(anyLong())).thenReturn(Optional.of(articleComment));
        when(articleRepository.findById(anyLong())).thenReturn(Optional.of(article));
        when(articleCommentRepository.getAllChildComment(anyLong())).thenReturn(ids);
        doNothing().when(articleCommentBatchService).deleteCommentBatch(anyLong(),eq(ids));

        ArticleCountResponse response = articleCommentService.deleteArticleComment(memberId, articleComment.getId());

        assertThat(response.getCount()).isEqualTo(0);
        verify(articleCommentBatchService, times(1)).deleteCommentBatch(anyLong(),anyList());
    }

    @DisplayName("자식 댓글 삭제")
    @Test
    void deleteChildComment(){
        ArticleComment replyComment = ArticleComment.builder().id(10L).article(article).childComments(new ArrayList<>()).parent(articleComment).member(member).build();

        when(articleCommentRepository.findById(anyLong())).thenReturn(Optional.ofNullable(replyComment));
        doNothing().when(articleCommentBatchService).deleteChildCommentBatch(anyLong());

        ArticleCountResponse response = articleCommentService.deleteArticleComment(memberId, articleId);

        assertThat(response).isNull();
        verify(articleCommentBatchService, times(1)).deleteChildCommentBatch(anyLong());
    }


}