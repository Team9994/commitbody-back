package team9499.commitbody.domain.like.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.dto.response.ArticleCountResponse;
import team9499.commitbody.domain.article.repository.ArticleRepository;
import team9499.commitbody.domain.comment.article.domain.ArticleComment;
import team9499.commitbody.domain.comment.article.repository.ArticleCommentRepository;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;
import team9499.commitbody.domain.comment.exercise.repository.ExerciseCommentRepository;
import team9499.commitbody.domain.like.domain.ContentLike;
import team9499.commitbody.domain.like.repository.LikeRepository;
import team9499.commitbody.domain.like.service.LikeService;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.notification.service.NotificationService;
import team9499.commitbody.global.redis.RedisService;

import java.util.*;

import static team9499.commitbody.global.Exception.ExceptionStatus.BAD_REQUEST;
import static team9499.commitbody.global.Exception.ExceptionType.*;
import static team9499.commitbody.global.constants.ElasticFiled.*;

@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRepository commentLikeRepository;
    private final ExerciseCommentRepository exerciseCommentRepository;
    private final ArticleRepository articleRepository;
    private final ArticleCommentRepository articleCommentRepository;
    private final RedisService redisService;
    private final NotificationService notificationService;
    private final LikeRepository likeRepository;

    /**
     * 운동 상세 페이지 - 댓글 좋아요 메서드
     *
     * @param exCommentId 댓글 ID
     * @param memberId    로그인한 상자 ID
     */
    public String exerciseCommentLike(Long exCommentId, Long memberId) {
        Member member = getRedisMember(memberId);
        ExerciseComment exerciseComment = exerciseCommentRepository.findById(exCommentId).get();
        Optional<ContentLike> optionalExerciseCommentLike = commentLikeRepository.findByMemberIdAndExerciseCommentId(memberId, exCommentId);
        return handleExerciseCommentLIke(optionalExerciseCommentLike, member, exerciseComment, exerciseComment.getLikeCount());
    }

    /**
     * 게시글의 좋아요를 하는 메서드
     *
     * @param articleId 좋아요할 게시글 ID
     * @param memberId  로그인한 사용자 ID
     */
    @Override
    public ArticleCountResponse articleLike(Long articleId, Long memberId) {
        Member member = getRedisMember(memberId);
        Optional<ContentLike> commentLikeOptional = commentLikeRepository.findByMemberIdAndArticleIdAndArticleCommentIdIsNull(memberId, articleId);
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, NO_SUCH_DATA));
        return handleArticleLike(articleId, commentLikeOptional, member, article);
    }

    /**
     * 게시글글의 댓글을 좋아요하는 기능
     *
     * @param memberId  로그인한 사용자 ID
     * @param commentId 좋아요할 댓글 ID
     */
    @Override
    public String articleCommentLike(Long memberId, Long commentId) {
        Optional<ContentLike> likeOptional = commentLikeRepository.findByMemberIdAndArticleCommentId(memberId, commentId);
        ArticleComment articleComment = articleCommentRepository.findById(commentId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, NO_SUCH_DATA));
        Member member = getRedisMember(memberId);

        if (likeOptional.isEmpty()) {
            return initArticleCommentLike(member, articleComment);
        }
        return updateArticleCommentLike(likeOptional.get(), articleComment, member);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Long> getWriteDrawArticleIds(Long memberId) {
        return likeRepository.findArticleIdsByDeleteMember(memberId);
    }

    private Member getRedisMember(Long memberId) {
        return redisService.getMemberDto(memberId.toString()).get();
    }

    private String handleExerciseCommentLIke(Optional<ContentLike> optionalExerciseCommentLike, Member member, ExerciseComment exerciseComment, Integer likeCount) {
        if (optionalExerciseCommentLike.isEmpty()) {
            return initCommentLike(member, exerciseComment, likeCount);
        }
        ContentLike exerciseCommentLike = optionalExerciseCommentLike.get();

        if (exerciseCommentLike.isLikeStatus()) {
            return updateLikeStatus(exerciseComment, exerciseCommentLike, likeCount, false);
        }
        return updateLikeStatus(exerciseComment, exerciseCommentLike, likeCount, true);
    }

    private String initCommentLike(Member member, ExerciseComment exerciseComment, Integer likeCount) {
        commentLikeRepository.save(ContentLike.createLike(member, exerciseComment));
        updateLikeCount(exerciseComment, likeCount, true);
        return ADD;
    }

    private void updateLikeCount(ExerciseComment exerciseComment, Integer likeCount, boolean type) {
        int updateCount = type ? 1 : -1;
        exerciseComment.updateLikeCount(likeCount + updateCount);
    }

    private String updateLikeStatus(ExerciseComment exerciseComment, ContentLike exerciseCommentLike, Integer likeCount, boolean type) {
        exerciseCommentLike.changeLike(type);
        updateLikeCount(exerciseComment, likeCount, type);
        exerciseComment.setLikeStatus(type);
        return type ? ADD : CANCEL;
    }

    private ArticleCountResponse handleArticleLike(Long articleId, Optional<ContentLike> commentLikeOptional, Member member, Article article) {
        if (commentLikeOptional.isEmpty()) {
            return initArticleLike(articleId, member, article);
        }
        ContentLike exerciseCommentLike = commentLikeOptional.get();
        return updateArticleLikeStatus(articleId, member, article, exerciseCommentLike);
    }

    private ArticleCountResponse initArticleLike(Long articleId, Member member, Article article) {
        ContentLike articleLike = ContentLike.createArticleLike(member, article);
        getUpdateLikeCount(article, true);
        commentLikeRepository.save(articleLike);
        notificationService.sendArticleLike(member, article.getMember().getId(), article.getId(), true);
        return ArticleCountResponse.of(articleId, article.getLikeCount(), ADD);
    }

    private ArticleCountResponse updateArticleLikeStatus(Long articleId, Member member, Article article, ContentLike exerciseCommentLike) {
        if (exerciseCommentLike.isLikeStatus()) {     // 만약 좋아요 상태가 true(좋아요 성공)이라면 false(좋아요 해제) 상태로 변경
            updateCommentLikeAndNotify(member, article, exerciseCommentLike, false);
            return ArticleCountResponse.of(articleId, article.getLikeCount(), CANCEL);
        }
        updateCommentLikeAndNotify(member, article, exerciseCommentLike, true);
        return ArticleCountResponse.of(articleId, article.getLikeCount(), ADD);
    }

    private void updateCommentLikeAndNotify(Member member, Article article, ContentLike exerciseCommentLike, boolean type) {
        getUpdateLikeCount(article, type);  // 게시글의 좋아요수를 -1
        exerciseCommentLike.changeLike(type);
        notificationService.sendArticleLike(member, article.getMember().getId(), article.getId(), type);
    }

    private String initArticleCommentLike(Member member, ArticleComment articleComment) {
        likeRepository.save(ContentLike.creatArticleCommentLike(member, articleComment));
        getUpdateLikeCount(articleComment, true);
        sendCommentLike(member, articleComment, articleComment.getArticle().getId(), true);
        return ADD;
    }

    private String updateArticleCommentLike(ContentLike contentLike, ArticleComment articleComment, Member member) {
        if (contentLike.isLikeStatus()) {     // 만약 좋아요 상태가 true(좋아요 성공)이라면 false(좋아요 해제) 상태로 변경
            updateCommentLikeAndNotify(member,articleComment,contentLike,false);
            return CANCEL;
        }     // 좋아요 상태가 false(취소) 상태라면 true(좋아요 성공)상태로 변경
        updateCommentLikeAndNotify(member,articleComment,contentLike,true);
        return ADD;
    }

    private void updateCommentLikeAndNotify(Member member, ArticleComment articleComment, ContentLike contentLike, boolean type) {
        getUpdateLikeCount(articleComment, type);
        contentLike.changeLike(type);
        sendCommentLike(member, articleComment, articleComment.getArticle().getId(), type);
    }

    /*
    게시글의 좋아요 수를 조절하는 메서드
    type true = 일때 +1 , false = -1
     */
    private static void getUpdateLikeCount(Object object, boolean type) {
        if (object instanceof Article) {
            Integer count = type ? ((Article) object).getLikeCount() + 1 : ((Article) object).getLikeCount() - 1;
            ((Article) object).updateLikeCount(count);
        } else if (object instanceof ArticleComment) {
            Integer count = type ? ((ArticleComment) object).getLikeCount() + 1 : ((ArticleComment) object).getLikeCount() - 1;
            ((ArticleComment) object).updateLikeCount(count);
        }
    }

    /*
    댓글 좋아요 알림 전송
     */
    private void sendCommentLike(Member member, ArticleComment articleComment, Long articleId, boolean status) {
        notificationService.sendCommentLike(member, articleComment.getMember().getId(), articleComment.getId(), articleId, status);
    }
}
