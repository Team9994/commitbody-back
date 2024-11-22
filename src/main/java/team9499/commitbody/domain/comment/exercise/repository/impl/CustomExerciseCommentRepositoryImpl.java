package team9499.commitbody.domain.comment.exercise.repository.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.block.domain.QBlockMember;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;
import team9499.commitbody.domain.comment.exercise.dto.ExerciseCommentDto;
import team9499.commitbody.domain.comment.exercise.repository.CustomExerciseCommentRepository;
import team9499.commitbody.global.utils.TimeConverter;

import java.util.List;
import java.util.stream.Collectors;

import static team9499.commitbody.domain.block.domain.QBlockMember.*;
import static team9499.commitbody.domain.comment.exercise.domain.QExerciseComment.*;
import static team9499.commitbody.domain.like.domain.QContentLike.*;
import static team9499.commitbody.global.constants.ElasticFiled.*;

@Repository
@RequiredArgsConstructor
public class CustomExerciseCommentRepositoryImpl implements CustomExerciseCommentRepository {

    private final JPAQueryFactory jpaQueryFactory;

    private final QBlockMember fromBlock = new QBlockMember("fromBlock");

    /**
     * 운동 댓글의 무한 스크롤 메서드
     *
     * @param memberId   로그인한 사용자 ID
     * @param exerciseId 운동 ID
     * @param source     운동 타입 [default : 일반 운동, custom : 사용자 작성 운동]
     * @param pageable   페이징 정보
     * @param lastId     마지막 ID 값
     * @return Slice 객체를 반환
     */
    @Override
    public Slice<ExerciseCommentDto> getExerciseComments(Long memberId, Long exerciseId, String source,
                                                         Pageable pageable, Long lastId) {
        BooleanBuilder builder = getBuilder(exerciseId, source, lastId);
        List<ExerciseComment> exerciseCommentsQuery = getExerciseCommentsQuery(memberId, pageable, builder);
        List<ExerciseCommentDto> commentDtos = getExerciseCommentDtos(memberId, exerciseCommentsQuery);
        return new SliceImpl<>(commentDtos, pageable, isHasNext(pageable, commentDtos));
    }

    private BooleanBuilder getBuilder(Long exerciseId, String source, Long lastId) {
        BooleanBuilder builder = new BooleanBuilder();
        builderLastId(lastId, builder);
        builderExerciseType(exerciseId, source, builder);
        return builder;
    }

    private static void builderLastId(Long lastId, BooleanBuilder builder) {
        if (lastId != null) {
            builder.and(exerciseComment.id.lt(lastId));
        }
    }

    private void builderExerciseType(Long exerciseId, String source, BooleanBuilder builder) {
        if (checkExerciseType(source)) {
            builder.and(exerciseComment.exercise.id.eq(exerciseId));
            return;
        }
        builder.and(exerciseComment.customExercise.id.eq(exerciseId));
    }

    private List<ExerciseComment> getExerciseCommentsQuery(Long memberId, Pageable pageable, BooleanBuilder builder) {
        return jpaQueryFactory.select(exerciseComment)
                .from(exerciseComment)
                .leftJoin(exerciseComment.exerciseCommentLikes, contentLike).fetchJoin()
                .leftJoin(blockMember).on(blockMember.blocked.id.eq(exerciseComment.member.id)
                        .and(blockMember.blocker.id.eq(memberId)))
                .leftJoin(fromBlock).on(fromBlock.blocked.id.eq(memberId)
                        .and(fromBlock.blocker.id.eq(exerciseComment.member.id)))
                .where(builder, blockMember.id.isNull().or(blockMember.blockStatus.eq(false))
                        .and(fromBlock.id.isNull().or(fromBlock.blockStatus.eq(false)))
                        .and(exerciseComment.member.isWithdrawn.eq(false)))
                .limit(pageable.getPageSize() + 1)
                .orderBy(exerciseComment.createdAt.desc())      // 최신순을 유지하기 위해 내림차순
                .fetch();
    }

    private static boolean isHasNext(Pageable pageable, List<ExerciseCommentDto> commentDtoList) {
        boolean hasNext = false;
        if (commentDtoList.size() > pageable.getPageSize()) {
            commentDtoList.remove(pageable.getPageSize());
            hasNext = true;
        }
        return hasNext;
    }

    private List<ExerciseCommentDto> getExerciseCommentDtos(Long memberId, List<ExerciseComment> exerciseComments) {
        return exerciseComments.stream()
                .map(ec -> createCommentDto(memberId, ec))
                .collect(Collectors.toList());
    }

    private ExerciseCommentDto createCommentDto(Long memberId, ExerciseComment ec) {
        return ExerciseCommentDto.of(
                ec.getId(),
                ec.getMember().getNickname(),
                ec.getContent(),
                TimeConverter.converter(ec.getCreatedAt()),
                checkAuthor(memberId, ec.getMember().getId()),
                ec.getLikeCount(),
                isLikeStatus(memberId, ec)
        );
    }

    private static boolean isLikeStatus(Long memberId, ExerciseComment ec) {
        return ec.getExerciseCommentLikes() != null && isCheckLike(memberId, ec);
    }

    private static boolean isCheckLike(Long memberId, ExerciseComment ec) {
        return ec.getExerciseCommentLikes()
                .stream()
                .anyMatch(like -> like.getMember().getId().equals(memberId) && like.isLikeStatus());
    }
    /*
    댓글이 현재 로그인한 사용자인지 확인하는 메서드
    로그인한 사용자가 작성자 일시 : true, 아닐시 : false
     */
    private boolean checkAuthor(Long loginMemberId, Long writer) {
        return loginMemberId.equals(writer);
    }
    /*
    운동 제공 타입 검사하는 메서드
    기본 제공 운동 : true , 커스텀 운동 : false
     */
    private boolean checkExerciseType(String source) {
        return source.equals(DEFAULT);
    }
}
