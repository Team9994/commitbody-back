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


    private final String ADD = "등록";
    private final String CANCEL ="해제";

    /**
     * 운동 상세 페이지 - 댓글 좋아요 메서드
     * @param exCommentId 댓글 ID
     * @param memberId  로그인한 상자 ID
     * @return
     */
    public String exerciseCommentLike(Long exCommentId, Long memberId) {

        Member member = redisService.getMemberDto(memberId.toString()).get();

        ExerciseComment exerciseComment = exerciseCommentRepository.findById(exCommentId).get();

        Optional<ContentLike> optionalExerciseCommentLike = commentLikeRepository.findByMemberIdAndExerciseCommentId(memberId, exCommentId);

        Integer likeCount = exerciseComment.getLikeCount();

        // 새로운 좋아요를 누른다면 새로운 객체 생성
        if (optionalExerciseCommentLike.isEmpty()){
          commentLikeRepository.save(ContentLike.createLike(member,exerciseComment));
          exerciseComment.updateLikeCount(likeCount + 1);
          return ADD;
        }

        ContentLike exerciseCommentLike = optionalExerciseCommentLike.get();

        // 좋아요 되어 잇다면 해제 좋아요 상태를 false로 바꾸며 좋아요 수 -1
        if (exerciseCommentLike.isLikeStatus()) {
            exerciseCommentLike.changeLike(false);       // 관심 해제
            exerciseComment.updateLikeCount(likeCount - 1);
            exerciseComment.setLikeStatus(false);
           return CANCEL;
        } else {    // 좋아요가 되어 있지않다면 상태를 true로 바꾸며, 좋아요 수 +1
            exerciseCommentLike.changeLike(true);        // 관심 등록
            exerciseComment.updateLikeCount(likeCount + 1);
            exerciseComment.setLikeStatus(true);
            return ADD;
        }
    }

    /**
     * 게시글의 좋아요를 하는 메서드
     * @param articleId 좋아요할 게시글 ID
     * @param memberId 로그인한 사용자 ID
     */
    @Override
    public ArticleCountResponse articleLike(Long articleId, Long memberId) {
        Optional<ContentLike> commentLikeOptional = commentLikeRepository.findByMemberIdAndArticleIdAndArticleCommentIdIsNull(memberId, articleId);
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, NO_SUCH_DATA));
        Member member = redisService.getMemberDto(String.valueOf(memberId)).get();


        String likeType = null;
        // 만약 좋아요한 데이터가 존재하지않는다면 좋아요를 ture를한 상태로 데이터 생성
        if (commentLikeOptional.isEmpty()){
            ContentLike articleLike = ContentLike.createArticleLike(member, article);
            getUpdateLikeCount(article,true);
            commentLikeRepository.save(articleLike);
            notificationService.sendArticleLike(member,article.getMember().getId(),article.getId(),true);
            likeType = ADD;
        }else{          // 만약 좋아요한 데이터자 존재할때
            ContentLike exerciseCommentLike = commentLikeOptional.get();
            if (exerciseCommentLike.isLikeStatus()) {     // 만약 좋아요 상태가 true(좋아요 성공)이라면 false(좋아요 해제) 상태로 변경
                getUpdateLikeCount(article,false);  // 게시글의 좋아요수를 -1
                exerciseCommentLike.changeLike(false);
                notificationService.sendArticleLike(member,article.getMember().getId(),article.getId(),false);
                likeType = CANCEL;
            }else {     // 좋아요 상태가 false(취소) 상태라면 true(좋아요 성공)상태로 변경
                exerciseCommentLike.changeLike(true);
                getUpdateLikeCount(article,true);
                notificationService.sendArticleLike(member,article.getMember().getId(),article.getId(),true);
                likeType = ADD;
            }
        }

        return ArticleCountResponse.of(articleId,article.getLikeCount(),likeType);
    }

    /**
     * 게시글글의 댓글을 좋아요하는 기능
     * @param memberId 로그인한 사용자 ID
     * @param commentId 좋아요할 댓글 ID
     * @return
     */
    @Override
    public String articleCommentLike(Long memberId, Long commentId) {
        Optional<ContentLike> likeOptional = commentLikeRepository.findByMemberIdAndArticleCommentId(memberId, commentId);
        ArticleComment articleComment = articleCommentRepository.findById(commentId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, NO_SUCH_DATA));
        Member member = redisService.getMemberDto(String.valueOf(memberId)).get();
        Long articleId = articleComment.getArticle().getId();
        // 댓글의 최초 좋아요시
        if (likeOptional.isEmpty()){
            ContentLike contentLike = ContentLike.creatArticleCommentLike(member, articleComment);
            likeRepository.save(contentLike);
            getUpdateLikeCount(articleComment, true);
            sendCommentLike(member, articleComment, articleId, true);
            return ADD;
        }else{
            ContentLike contentLike = likeOptional.get();
            if (contentLike.isLikeStatus()) {     // 만약 좋아요 상태가 true(좋아요 성공)이라면 false(좋아요 해제) 상태로 변경
                getUpdateLikeCount(articleComment,false);  // 게시글의 좋아요수를 -1
                contentLike.changeLike(false);
                sendCommentLike(member, articleComment, articleId, false);
                return CANCEL;
            }else {     // 좋아요 상태가 false(취소) 상태라면 true(좋아요 성공)상태로 변경
                getUpdateLikeCount(articleComment,true);
                contentLike.changeLike(true);
                sendCommentLike(member, articleComment, articleId, true);
                return ADD;
            }
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<Long> getWriteDrawArticleIds(Long memberId) {
        return likeRepository.findArticleIdsByDeleteMember(memberId);
    }

    /*
    게시글의 좋아요 수를 조절하는 메서드
    type true = 일때 +1 , false = -1
     */
    private static void getUpdateLikeCount(Object object,boolean type) {
        if (object instanceof  Article){
            Integer count = type ? ((Article) object).getLikeCount() + 1 : ((Article) object).getLikeCount()  -1;
            ((Article) object).updateLikeCount(count);
        }else if (object instanceof ArticleComment){
            Integer count = type ? ((ArticleComment) object).getLikeCount() + 1 : ((ArticleComment) object).getLikeCount()  -1;
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
