package team9499.commitbody.domain.record.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.record.domain.*;
import team9499.commitbody.domain.record.domain.Record;
import team9499.commitbody.domain.record.dto.response.RecordDetailsResponse;
import team9499.commitbody.domain.record.dto.response.RecordResponse;
import team9499.commitbody.domain.record.dto.response.RecordSetsResponse;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

import static team9499.commitbody.domain.record.domain.QRecord.*;
import static team9499.commitbody.domain.record.domain.QRecordDetails.*;
import static team9499.commitbody.domain.record.domain.QRecordSets.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomRecordRepositoryImpl implements CustomRecordRepository{

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * 루틴을 완료시 저장된 루틴 정보를 조회하기 위핸 쿼리
     * 사용자의 루틴만 볼 수 있다.
     * @param recordId  기록 ID
     * @param memberId  사용자 ID
     */
    @Override
    public RecordResponse findByRecordId(Long recordId, Long memberId) {

        List<Tuple> list = jpaQueryFactory.select(record,recordDetails,recordSets).from(record)
                .join(recordDetails).on(record.id.eq(recordDetails.record.id))
                .join(recordSets).on(recordSets.recordDetails.id.eq(recordDetails.id))
                .where(record.id.eq(recordId).and(record.member.id.eq(memberId)))
                .orderBy(recordDetails.orders.asc())
                .fetch();

        // Tuple 리스트를 Record를 키로 하고, RecordDetailsResponse 리스트를 값으로 가지는 Map으로 그룹화
        Map<Record, List<RecordDetailsResponse>> groupedDetails = list.stream().collect(Collectors.groupingBy(
                tuple -> tuple.get(0, Record.class),
                Collectors.mapping(
                        tuple -> {
                            // RecordDetails에서 Exercise와 CustomExercise의 null 처리
                            RecordDetails recordDetail = tuple.get(1, RecordDetails.class);
                            Exercise exercise = Optional.ofNullable(recordDetail.getExercise())
                                    .orElse(null);
                            CustomExercise customExercise = Optional.ofNullable(recordDetail.getCustomExercise())
                                    .orElse(null);
                            
                            // RecordDetailsResponse 객체를 생성하여 반환
                            return RecordDetailsResponse.of(
                                    recordDetail.getId(),
                                    exercise,
                                    customExercise,
                                    recordDetail.getDetailsReps(),
                                    recordDetail.getDetailsSets(),
                                    recordDetail.getSumTimes(),
                                    recordDetail.getDetailsVolume(),
                                    recordDetail.getMaxReps(),
                                    recordDetail.getMax1RM(),
                                    new ArrayList<>()
                            );
                        },
                        Collectors.toList()  // 생성된 RecordDetailsResponse를 리스트로 변환
                )
        ));

        // 그룹화된 Map의 엔트리를 스트림으로 변환하여 처리
        return groupedDetails.entrySet().stream().map(entry -> {
            Record record = entry.getKey(); // 각 엔트리의 키인 Record 객체
            List<RecordDetailsResponse> details = entry.getValue(); // 각 엔트리의 값인 RecordDetailsResponse 리스트

            // RecordDetailId를 키로 하고, RecordDetailsResponse를 값으로 가지는 Map을 생성
            Map<Long, RecordDetailsResponse> detailsMap = new LinkedHashMap<>();
            details.forEach(detail -> detailsMap.put(detail.getRecordDetailId(), detail));

            // 리스트를 다시 순회하며 RecordSets 정보를 RecordDetailsResponse에 추가
            list.stream().filter(tuple -> tuple.get(1, RecordDetails.class) != null)
                    .forEach(tuple -> {
                Long detailId = tuple.get(1, RecordDetails.class).getId();
                RecordSets recordSets = tuple.get(2, RecordSets.class);

                if (detailsMap.containsKey(detailId)) {      // 해당 detailId를 키로 가진 RecordDetailsResponse가 있으면
                    RecordDetailsResponse detail = detailsMap.get(detailId);
                    List<RecordSetsResponse> sets = detail.getSets();
                    if (sets == null) {
                        sets = new ArrayList<>();        // sets 리스트가 null인 경우 새로운 ArrayList로 초기화
                    }
                    sets.add(RecordSetsResponse.of(
                            recordSets.getId(),
                            recordSets.getWeight(),
                            recordSets.getReps(),
                            recordSets.getTimes()
                    )); // 현재의 RecordSets 정보를 RecordSetsResponse로 변환하여 sets 리스트에 추가
                    detail.setSets(sets);   // 업데이트된 sets 리스트를 RecordDetailsResponse에 설정
                }
            });

            return new RecordResponse(
                    record.getId(),
                    record.getRecordName(),
                    converterTime(record),
                    converterDurationTime(record),
                    record.getDuration(),
                    record.getRecordVolume(),
                    record.getRecordSets(),
                    record.getRecordCalorie(),
                    new ArrayList<>(detailsMap.values())
            );
        }).findFirst().orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }

    @Override
    public void deleteCustomExercise(Long customExerciseId) {
        jpaQueryFactory.delete(recordSets)
                .where(recordDetails.id.in(
                        JPAExpressions.select(recordDetails.id)
                                .from(recordDetails)
                                .where(recordDetails.customExercise.id.eq(customExerciseId)))
                ).execute();

        jpaQueryFactory.delete(recordDetails).where(recordDetails.customExercise.id.eq(customExerciseId)).execute();
    }

    @Override
    public void deleteRecord(Long recordId, Long memberId) {
        // 기록 세트 삭제
        jpaQueryFactory.delete(recordSets)
                .where(recordSets.recordDetails.id.in(
                        JPAExpressions.select(recordDetails.id)
                                .from(recordDetails)
                                .where(recordDetails.record.id.eq(recordId)))).execute();

        // 기록 상세 삭제
        jpaQueryFactory.delete(recordDetails)
                .where(recordDetails.record.id.eq(recordId))
                .execute();

        // 기록 삭제
            jpaQueryFactory.delete(record)
                    .where(record.id.eq(recordId).and(record.member.id.eq(memberId)))
                    .execute();
    }

    /*
    운동 시작 시간과 운동 끝시간을 18:14~20:15 로변환 하는메서드
     */
    private  String converterDurationTime(Record record) {
        StringBuilder sb =new StringBuilder();
        LocalDateTime startTime = record.getStartTime();
        LocalDateTime endTime = record.getEndTime();

        return sb.append(startTime.getHour()).append(":").append(startTime.getMinute()).append("~").append(endTime.getHour()).append(":").append(endTime.getMinute()).toString();
    }

    /*
    LocalDateTime을 2024.08.25.(금) 형식으로변경
     */
    private String converterTime(Record record) {
        LocalDateTime startTime = record.getStartTime();
        String string = LocalDate.of(startTime.getYear(), startTime.getMonth(), startTime.getDayOfMonth()).toString().toString().replace('-', '.');
        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        String displayName = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREA);
        return string + ".(" + displayName + ")";
    }
}
