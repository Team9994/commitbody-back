package team9499.commitbody.domain.comment.exercise.service;

import lombok.RequiredArgsConstructor;
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
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.global.Exception.NoSuchException;

import static team9499.commitbody.global.Exception.ExceptionStatus.*;
import static team9499.commitbody.global.Exception.ExceptionType.*;

@Service
@Transactional
@RequiredArgsConstructor
public class ExerciseCommentServiceImpl implements ExerciseCommentService{

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

}
