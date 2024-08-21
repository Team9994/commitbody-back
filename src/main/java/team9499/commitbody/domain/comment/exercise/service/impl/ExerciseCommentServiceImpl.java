package team9499.commitbody.domain.comment.exercise.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;
import team9499.commitbody.domain.comment.exercise.dto.ExerciseCommentDto;
import team9499.commitbody.domain.comment.exercise.dto.response.ExerciseCommentResponse;
import team9499.commitbody.domain.comment.exercise.repository.ExerciseCommentRepository;
import team9499.commitbody.domain.comment.exercise.service.ExerciseCommentService;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.Exception.NoSuchException;

import static team9499.commitbody.global.Exception.ExceptionStatus.*;
import static team9499.commitbody.global.Exception.ExceptionType.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ExerciseCommentServiceImpl implements ExerciseCommentService {

    private final ExerciseCommentRepository exerciseCommentRepository;
    private final ExerciseRepository exerciseRepository;
    private final CustomExerciseRepository customExerciseRepository;
    private final MemberRepository memberRepository;

    @Override
    public void saveExerciseComment(Long memberId, Long exerciseId, String source, String comment) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, No_SUCH_MEMBER));
        Object exercise;
        if (source.equals("default")){
            exercise = exerciseRepository.findById(exerciseId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, NO_SUCH_DATA));
        }else
            exercise = customExerciseRepository.findById(exerciseId).orElseThrow(() -> new NoSuchException(BAD_REQUEST,NO_SUCH_DATA));

        exerciseCommentRepository.save(ExerciseComment.of(member,exercise,comment));
    }

    /**
     * 운동 댓글 무한 스크롤 조회
     */
    @Transactional(readOnly = true)
    @Override
    public ExerciseCommentResponse getExerciseComments(Long memberId, Long exerciseId, String source, Pageable pageable, Long lastId) {
        Slice<ExerciseCommentDto> exerciseComments = exerciseCommentRepository.getExerciseComments(memberId, exerciseId, source, pageable, lastId);
        return new ExerciseCommentResponse(exerciseComments.hasNext(),exerciseComments.getContent());
    }

    /**
     * 운동 댓글 삭제 - 작성자만 해당 댓글을 삭제 가능합니다.
     * @param memberId 로그인한 사용자 ID
     * @param exerciseCommentId 삭제할 운동 댓글 ID
     */
    @Override
    public void deleteExerciseComment(Long memberId, Long exerciseCommentId) {
        // 운동 댓글 ID를 통해 데이터를 조회 후 현재 로그인한 사용자 작성한 데이터가 403 오류가 발생한다. 
        exerciseCommentRepository.findById(exerciseCommentId).filter(exerciseComment -> {
            if (!exerciseComment.getMember().getId().equals(memberId))
                throw new InvalidUsageException(FORBIDDEN,AUTHOR_ONLY);
            return true;
        }).orElseThrow(() -> new NoSuchException(BAD_REQUEST,NO_SUCH_DATA));
        // 운동 댓글, 운동 좋아요 목록 삭제
        exerciseCommentRepository.deleteByMemberIdAndId(memberId,exerciseCommentId);
    }

}
