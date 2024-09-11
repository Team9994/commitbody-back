package team9499.commitbody.domain.like.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.repository.ArticleRepository;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;
import team9499.commitbody.domain.comment.exercise.repository.ExerciseCommentRepository;
import team9499.commitbody.domain.like.domain.ContentLike;
import team9499.commitbody.domain.like.repository.LikeRepository;
import team9499.commitbody.domain.like.service.LikeService;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.redis.RedisService;

import java.util.Optional;

import static team9499.commitbody.global.Exception.ExceptionStatus.BAD_REQUEST;
import static team9499.commitbody.global.Exception.ExceptionType.*;

@Service
@Transactional
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRepository commentLikeRepository;
    private final ExerciseCommentRepository exerciseCommentRepository;
    private final ArticleRepository articleRepository;
    private final RedisService redisService;
    private final MemberRepository memberRepository;

    private final String ADD = "등록";
    private final String CANCEL ="해제";

    /**
     * 운동 상세 페이지 - 댓글 좋아요 메서드
     * @param exCommentId 댓글 ID
     * @param memberId  로그인한 상자 ID
     * @return
     */
    public String updateCommentLike(Long exCommentId, Long memberId) {

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, No_SUCH_MEMBER));

        ExerciseComment exerciseComment = exerciseCommentRepository.findById(exCommentId).get();

        Optional<ContentLike> optionalExerciseCommentLike = commentLikeRepository.findByMemberIdAndExerciseCommentId(memberId, exCommentId);
        
        // 새로운 좋아요를 누른다면 새로운 객체 생성
        if (optionalExerciseCommentLike.isEmpty()){
            optionalExerciseCommentLike = Optional.of(commentLikeRepository.save(ContentLike.createLike(member,exerciseComment)));
        }

        ContentLike exerciseCommentLike = optionalExerciseCommentLike.get();
        Integer likeCount = exerciseComment.getLikeCount();

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
    public String articleLike(Long articleId, Long memberId) {
        Optional<ContentLike> commentLikeOptional = commentLikeRepository.findByMemberIdAndArticleIdAndExerciseCommentIsNull(memberId, articleId);
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, NO_SUCH_DATA));
        // 만약 좋아요한 데이터가 존재하지않는다면 좋아요를 ture를한 상태로 데이터 생성
        if (commentLikeOptional.isEmpty()){
            Member member = redisService.getMemberDto(String.valueOf(memberId)).get();
            ContentLike articleLike = ContentLike.createArticleLike(member, article);
            getUpdateLikeCount(article,true);
            commentLikeRepository.save(articleLike);
            return ADD;
        }else{          // 만약 좋아요한 데이터자 존재할때
            ContentLike exerciseCommentLike = commentLikeOptional.get();
            if (exerciseCommentLike.isLikeStatus()) {     // 만약 좋아요 상태가 true(좋아요 성공)이라면 false(좋아요 해제) 상태로 변경
                getUpdateLikeCount(article,false);  // 게시글의 좋아요수를 -1
                exerciseCommentLike.changeLike(false);
                return CANCEL;
            }else {     // 좋아요 상태가 false(취소) 상태라면 true(좋아요 성공)상태로 변경
                exerciseCommentLike.changeLike(true);
                getUpdateLikeCount(article,true);
                return ADD;
            }

        }
    }

    /*
    게시글의 좋아요 수를 조절하는 메서드
    type true = 일때 +1 , false = -1
     */
    private static void getUpdateLikeCount(Article article,boolean type) {
        Integer count = type ? article.getLikeCount() + 1 : article.getLikeCount()  -1;
        article.updateLikeCount(count);
    }
}
