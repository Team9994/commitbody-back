package team9499.commitbody.domain.like.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.dto.response.ArticleCountResponse;
import team9499.commitbody.domain.article.repository.ArticleRepository;
import team9499.commitbody.domain.comment.article.domain.ArticleComment;
import team9499.commitbody.domain.comment.article.repository.ArticleCommentRepository;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;
import team9499.commitbody.domain.comment.exercise.repository.ExerciseCommentRepository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.like.domain.ContentLike;
import team9499.commitbody.domain.like.repository.LikeRepository;
import team9499.commitbody.global.notification.service.NotificationService;
import team9499.commitbody.global.redis.RedisService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

    @Mock private LikeRepository likeRepository;
    @Mock private ExerciseCommentRepository exerciseCommentRepository;
    @Mock private ArticleRepository articleRepository;
    @Mock private ArticleCommentRepository articleCommentRepository;
    @Mock private RedisService redisService;
    @Mock private NotificationService notificationService;

    @InjectMocks private LikeServiceImpl likeService;


    private Long memberId = 1L;
    private Member member;

    @BeforeEach
    void init(){
        member = Member.builder().id(memberId).nickname("사용자1").build();
    }

    @DisplayName("운동 댓글 좋아요 - 최초 등록시")
    @Test
    void exerciseCommentLike(){
        CustomExercise customExercise = CustomExercise.builder().customExName("커스텀 운동").build();
        ExerciseComment exerciseComment = ExerciseComment.builder().id(2L).customExercise(customExercise).likeCount(0).member(member).content("커스텀 운동 댓글1").build();

        when(redisService.getMemberDto(memberId.toString())).thenReturn(Optional.of(member));
        when(exerciseCommentRepository.findById(any())).thenReturn(Optional.of(exerciseComment));
        when(likeRepository.findByMemberIdAndExerciseCommentId(anyLong(),anyLong())).thenReturn(Optional.empty());
        when(likeRepository.save(any())).thenReturn(ContentLike.createLike(member,exerciseComment));

        String status = likeService.exerciseCommentLike(exerciseComment.getId(), memberId);
        assertThat(status).isEqualTo("등록");     //최초 좋아요
        assertThat(exerciseComment.getLikeCount()).isEqualTo(1);    // 좋아요 증가
    }

    @DisplayName("운동 댓글 좋아요 - 좋아요 해제시")
    @Test
    void exerciseCancelCommentLike(){
        CustomExercise customExercise = CustomExercise.builder().customExName("커스텀 운동").build();
        ExerciseComment exerciseComment = ExerciseComment.builder().id(2L).likeCount(10).customExercise(customExercise).member(member).content("커스텀 운동 댓글1").build();
        ContentLike contentLike = ContentLike.builder().likeStatus(true).exerciseComment(exerciseComment).member(member).build();

        when(redisService.getMemberDto(memberId.toString())).thenReturn(Optional.of(member));
        when(exerciseCommentRepository.findById(any())).thenReturn(Optional.of(exerciseComment));
        when(likeRepository.findByMemberIdAndExerciseCommentId(eq(memberId),eq(exerciseComment.getId()))).thenReturn(Optional.of(contentLike));

        String status = likeService.exerciseCommentLike(exerciseComment.getId(), memberId);
        assertThat(status).isEqualTo("해제");
        assertThat(exerciseComment.getLikeCount()).isEqualTo(9);    //좋아요 감소
    }
    
    @DisplayName("운동 댓글 좋아요 - 좋아요 객체 존재시 좋아요")
    @Test
    void exerciseReCommentLike(){
        CustomExercise customExercise = CustomExercise.builder().customExName("커스텀 운동").build();
        ExerciseComment exerciseComment = ExerciseComment.builder().id(2L).likeCount(10).customExercise(customExercise).member(member).content("커스텀 운동 댓글1").build();
        ContentLike contentLike = ContentLike.builder().likeStatus(false).exerciseComment(exerciseComment).member(member).build();

        when(redisService.getMemberDto(memberId.toString())).thenReturn(Optional.of(member));
        when(exerciseCommentRepository.findById(any())).thenReturn(Optional.of(exerciseComment));
        when(likeRepository.findByMemberIdAndExerciseCommentId(eq(memberId),eq(exerciseComment.getId()))).thenReturn(Optional.of(contentLike));

        String status = likeService.exerciseCommentLike(exerciseComment.getId(), memberId);
        assertThat(status).isEqualTo("등록");
        assertThat(exerciseComment.getLikeCount()).isEqualTo(11);       // 좋아요 수 증가
    }
    
    @DisplayName("게시글 좋아요 - 최초 좋아요")
    @Test
    void articleInitLike(){
        Article article = Article.builder().id(1L).title("제목").likeCount(2).member(member).build();
        ContentLike contentLike = ContentLike.createArticleLike(member, article);

        when(likeRepository.findByMemberIdAndArticleIdAndArticleCommentIdIsNull(anyLong(),anyLong())).thenReturn(Optional.empty());
        when(articleRepository.findById(anyLong())).thenReturn(Optional.of(article));
        when(redisService.getMemberDto(eq(memberId.toString()))).thenReturn(Optional.of(member));
        when(likeRepository.save(any())).thenReturn(contentLike);
        doNothing().when(notificationService).sendArticleLike(any(),anyLong(),anyLong(),eq(true));

        ArticleCountResponse response = likeService.articleLike(article.getId(), memberId);
        assertThat(response.getCount()).isEqualTo(3);
        assertThat(response.getType()).isEqualTo("등록");
        verify(notificationService,times(1)).sendArticleLike(any(),anyLong(),anyLong(),eq(true));
    }

    @DisplayName("게시글 좋아요 - 좋아요 해제")
    @Test
    void articleCancelLike(){
        Article article = Article.builder().id(1L).title("제목").likeCount(2).member(member).build();
        ContentLike contentLike = ContentLike.createArticleLike(member, article);

        when(likeRepository.findByMemberIdAndArticleIdAndArticleCommentIdIsNull(anyLong(),anyLong())).thenReturn(Optional.of(contentLike));
        when(articleRepository.findById(anyLong())).thenReturn(Optional.of(article));
        when(redisService.getMemberDto(eq(memberId.toString()))).thenReturn(Optional.of(member));
        doNothing().when(notificationService).sendArticleLike(any(),anyLong(),anyLong(),eq(false));

        ArticleCountResponse response = likeService.articleLike(article.getId(), memberId);
        assertThat(response.getCount()).isEqualTo(1);
        assertThat(response.getType()).isEqualTo("해제");
        verify(notificationService,times(1)).sendArticleLike(any(),anyLong(),anyLong(),eq(false));
    }

    @DisplayName("게시글 좋아요 - 좋아요 데이터 존재시 좋아요")
    @Test
    void articleReLike(){
        Article article = Article.builder().id(1L).title("제목").likeCount(2).member(member).build();
        ContentLike contentLike = ContentLike.builder().likeStatus(false).article(article).member(member).build();

        when(likeRepository.findByMemberIdAndArticleIdAndArticleCommentIdIsNull(anyLong(),anyLong())).thenReturn(Optional.of(contentLike));
        when(articleRepository.findById(anyLong())).thenReturn(Optional.of(article));
        when(redisService.getMemberDto(eq(memberId.toString()))).thenReturn(Optional.of(member));
        doNothing().when(notificationService).sendArticleLike(any(),anyLong(),anyLong(),eq(true));

        ArticleCountResponse response = likeService.articleLike(article.getId(), memberId);
        assertThat(response.getCount()).isEqualTo(3);
        assertThat(response.getType()).isEqualTo("등록");
        verify(notificationService,times(1)).sendArticleLike(any(),anyLong(),anyLong(),eq(true));
    }



    @DisplayName("게시글 댓글 좋아요 - 최초 좋아요")
    @Test
    void articleCommentInitLike(){
        Article article = Article.builder().id(2L).title("게시글1").likeCount(1).build();
        ArticleComment articleComment = ArticleComment.builder().id(3L).article(article).member(member).likeCount(2).parent(null).build();
        ContentLike articleCommentLike = ContentLike.creatArticleCommentLike(member, articleComment);

        when(likeRepository.findByMemberIdAndArticleCommentId(anyLong(),anyLong())).thenReturn(Optional.empty());
        when(articleCommentRepository.findById(anyLong())).thenReturn(Optional.of(articleComment));
        when(redisService.getMemberDto(eq(memberId.toString()))).thenReturn(Optional.of(member));
        when(likeRepository.save(any())).thenReturn(articleCommentLike);
        doNothing().when(notificationService).sendCommentLike(eq(member),anyLong(),anyLong(),anyLong(),eq(true));

        String status = likeService.articleCommentLike(memberId,3L);

        assertThat(status).isEqualTo("등록");
        assertThat(articleComment.getLikeCount()).isEqualTo(3);
        verify(notificationService,times(1)).sendCommentLike(any(),anyLong(),anyLong(),anyLong(),anyBoolean());
    }

    @DisplayName("게시글 댓글 좋아요 - 좋아요 해제")
    @Test
    void articleCommentCancelLike(){
        Article article = Article.builder().id(2L).title("게시글1").likeCount(1).build();
        ArticleComment articleComment = ArticleComment.builder().id(3L).article(article).member(member).likeCount(2).parent(null).build();
        ContentLike articleCommentLike = ContentLike.creatArticleCommentLike(member, articleComment);

        when(likeRepository.findByMemberIdAndArticleCommentId(anyLong(),anyLong())).thenReturn(Optional.of(articleCommentLike));
        when(redisService.getMemberDto(eq(memberId.toString()))).thenReturn(Optional.of(member));
        when(articleCommentRepository.findById(anyLong())).thenReturn(Optional.of(articleComment));
        doNothing().when(notificationService).sendCommentLike(eq(member),anyLong(),anyLong(),anyLong(),eq(false));

        String status = likeService.articleCommentLike(memberId, 3L);
        assertThat(status).isEqualTo("해제");
        assertThat(articleComment.getLikeCount()).isEqualTo(1);
        verify(notificationService,times(1)).sendCommentLike(any(),anyLong(),anyLong(),anyLong(),anyBoolean());

    }

    @DisplayName("게시글 댓글 좋아요 - 좋아요 데이터 존재시 좋아요")
    @Test
    void articleCommentReLike(){
        Article article = Article.builder().id(2L).title("게시글1").likeCount(1).build();
        ArticleComment articleComment = ArticleComment.builder().id(3L).article(article).member(member).likeCount(2).parent(null).build();
        ContentLike contentLike = ContentLike.builder().member(member).articleComment(articleComment).likeStatus(false).build();

        when(likeRepository.findByMemberIdAndArticleCommentId(anyLong(),anyLong())).thenReturn(Optional.of(contentLike));
        when(redisService.getMemberDto(eq(memberId.toString()))).thenReturn(Optional.of(member));
        when(articleCommentRepository.findById(anyLong())).thenReturn(Optional.of(articleComment));
        doNothing().when(notificationService).sendCommentLike(eq(member),anyLong(),anyLong(),anyLong(),eq(true));

        String status = likeService.articleCommentLike(memberId, 3L);
        assertThat(status).isEqualTo("등록");
        assertThat(articleComment.getLikeCount()).isEqualTo(3);
        verify(notificationService,times(1)).sendCommentLike(any(),anyLong(),anyLong(),anyLong(),anyBoolean());
    }


}