package team9499.commitbody.domain.comment.exercise.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.comment.exercise.domain.ExerciseComment;
import team9499.commitbody.domain.comment.exercise.dto.ExerciseCommentDto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static team9499.commitbody.domain.comment.exercise.domain.QExerciseComment.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomExerciseCommentRepositoryImpl implements CustomExerciseCommentRepository{

    private final JPAQueryFactory jpaQueryFactory;

    public static final int SEC = 60;
    public static final int MIN = 60;
    public static final int HOUR = 24;
    public static final int DAY = 30;
    public static final int MONTH = 12;

    /**
     * 운동 댓글의 무한 스크롤 메서드
     * @param memberId  로그인한 사용자 ID
     * @param exerciseId    운동 ID
     * @param source    운동 타입 [default : 일반 운동, custom : 사용자 작성 운동]
     * @param pageable  페이징 정보
     * @param lastId    마지막 ID 값
     * @return  Slice 객체를 반환
     */
    @Override
    public Slice<ExerciseCommentDto> getExerciseComments(Long memberId, Long exerciseId, String source, Pageable pageable, Long lastId) {
        BooleanBuilder builder = new BooleanBuilder();

        if (lastId!=null) builder.and(exerciseComment.id.lt(lastId));       // 무한 스크롤 다음 페이지가 있을시
        if (checkExerciseType(source)) builder.and(exerciseComment.exercise.id.eq(exerciseId)); //기본 운동일 경우
        else builder.and(exerciseComment.customExercise.id.eq(exerciseId));     // 커스텀 운동일 경우

        // 쿼리 실행
        List<ExerciseComment> exerciseComments = jpaQueryFactory.select(exerciseComment)
                .from(exerciseComment)
                .where(builder)
                .limit(pageable.getPageSize()+1)
                .orderBy(exerciseComment.createdAt.desc())      // 최신순을 유지하기 위해 내림차순
                .fetch();

        List<ExerciseCommentDto>  commentDtoList = exerciseComments.stream()
                .map(ec -> ExerciseCommentDto.of(ec.getId(), ec.getContent(), converter(ec.getCreatedAt()), checkAuthor(memberId,ec.getMember().getId()),ec.getLikeCount()))
                .collect(Collectors.toList());

        // 다음 페이직 존재하는지 검사
        boolean hasNext = false;
        if ( commentDtoList.size() > pageable.getPageSize()){
            commentDtoList.remove(pageable.getPageSize());
            hasNext = true;
        }

       return new SliceImpl<>(commentDtoList,pageable,hasNext);
    }

    /*
    댓글이 현재 로그인한 사용자인지 확인하는 메서드
    로그인한 사용자가 작성자 일시 : true, 아닐시 : false
     */
    private boolean checkAuthor(Long loginMemberId, Long writer) {
        return loginMemberId==writer ? true : false;
    }

    /*
    수정된일 기준으로 1일전 ,2주전을 표현학 위한 시간 컨버터
     */
    private String converter(LocalDateTime updatedAt) {
        LocalDateTime now = LocalDateTime.now();

        long diffTime = updatedAt.until(now, ChronoUnit.SECONDS); // now보다 이후면 +, 전이면 -

        if (diffTime < SEC){
            return diffTime + "초전";
        }
        diffTime = diffTime / SEC;
        if (diffTime < MIN) {
            return diffTime + "분 전";
        }
        diffTime = diffTime / MIN;
        if (diffTime < HOUR) {
            return diffTime + "시간 전";
        }
        diffTime = diffTime / HOUR;
        if (diffTime < DAY) {
            return diffTime + "일 전";
        }
        diffTime = diffTime / DAY;
        if (diffTime < MONTH) {
            return diffTime + "개월 전";
        }

        diffTime = diffTime / MONTH;
        return diffTime + "년 전";
    }

    /*
    운동 제공 타입 검사하는 메서드
    기본 제공 운동 : true , 커스텀 운동 : false
     */
    private boolean checkExerciseType(String source){
        return source.equals("default") ? true : false;
    }
}
