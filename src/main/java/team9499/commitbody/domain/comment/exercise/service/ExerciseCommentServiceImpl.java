package team9499.commitbody.domain.comment.exercise.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;
import team9499.commitbody.domain.comment.exercise.repository.ExerciseCommentRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;

@Service
@Transactional
@RequiredArgsConstructor
public class ExerciseCommentServiceImpl implements ExerciseCommentService{

    private final ExerciseCommentRepository exerciseCommentRepository;
    private final MemberRepository memberRepository;

    @Override
    public void saveExerciseComment(Long memberId, String comment) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
        exerciseCommentRepository.save(ExerciseComment.of(member,comment));
    }
}
