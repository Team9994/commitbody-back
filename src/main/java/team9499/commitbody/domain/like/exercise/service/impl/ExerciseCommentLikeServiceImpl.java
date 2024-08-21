package team9499.commitbody.domain.like.exercise.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;
import team9499.commitbody.domain.comment.exercise.repository.ExerciseCommentRepository;
import team9499.commitbody.domain.like.exercise.domain.ExerciseCommentLike;
import team9499.commitbody.domain.like.exercise.repository.ExerciseCommentLikeRepository;
import team9499.commitbody.domain.like.exercise.service.ExerciseCommentLikeService;
import team9499.commitbody.global.Exception.NoSuchException;

import java.util.Optional;

import static team9499.commitbody.global.Exception.ExceptionStatus.BAD_REQUEST;
import static team9499.commitbody.global.Exception.ExceptionType.*;

@Service
@Transactional
@RequiredArgsConstructor
public class ExerciseCommentLikeServiceImpl implements ExerciseCommentLikeService {

    private final ExerciseCommentLikeRepository exerciseCommentLikeRepository;
    private final ExerciseCommentRepository exerciseCommentRepository;
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

        Optional<ExerciseCommentLike> optionalExerciseCommentLike = exerciseCommentLikeRepository.findByMemberIdAndExerciseCommentId(memberId, exCommentId);
        
        // 새로운 좋아요를 누른다면 새로운 객체 생성
        if (optionalExerciseCommentLike.isEmpty()){
            optionalExerciseCommentLike = Optional.of(exerciseCommentLikeRepository.save(ExerciseCommentLike.createLike(member,exerciseComment)));
        }

        ExerciseCommentLike exerciseCommentLike = optionalExerciseCommentLike.get();
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
}
