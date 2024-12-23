package team9499.commitbody.domain.record.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static team9499.commitbody.domain.record.domain.QRecord.*;
import static team9499.commitbody.domain.record.domain.QRecordDetails.*;
import static team9499.commitbody.domain.record.domain.QRecordSets.*;
import static team9499.commitbody.domain.record.dto.response.RecordMonthResponse.*;
import static team9499.commitbody.global.constants.ElasticFiled.*;
import static team9499.commitbody.global.utils.TimeConverter.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomRecordRepositoryImpl implements CustomRecordRepository {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * 루틴을 완료시 저장된 루틴 정보를 조회하기 위핸 쿼리
     * 사용자의 루틴만 볼 수 있다.
     */
    @Override
    public RecordResponse findByRecordId(Long recordId, Long memberId) {
        List<Tuple> list = fetchRecordDataWithDetailsAndSets(recordId, memberId);
        Map<Record, List<RecordDetailsResponse>> groupedDetails = groupRecordsWithDetails(list);
        return handleRecordResponse(groupedDetails, list);
    }

    @Override
    public void deleteCustomExercise(Long customExerciseId) {
        deleteRecordSetsByCustomExercise(customExerciseId);
        deleteRecordDetailsByCustomExercise(customExerciseId);
    }

    @Override
    public void deleteRecord(Long recordId, Long memberId) {
        deleteRecordSets(recordId);
        deleteRecordDetails(recordId);
        deleteRecords(recordId, memberId);
    }

    /**
     * 기록페이지에서 해당 달의 일별로 진행한 기록을 조회
     */
    @Override
    public Map<String, RecordData> getRecordCountAdnDataForMonth(Long memberId, Integer year, Integer month) {
        List<Record> records = fetchRecordsByMonth(memberId, year, month);
        return mapRecordsByDay(records);
    }

    /**
     * 해당달의 진행만 모든 기록을 무한 스크롤 조회
     */
    @Override
    public List<RecordDay> getRecordPage(Long memberId, Integer year, Integer month) {
        List<Record> records = fetchRecordsForMonth(memberId, year, month);
        return mapToRecordDays(records);
    }

    @Override
    public Map<LocalDate, List<RecordSetsResponse>> getRecentRecordsByExercise(Long exerciseId, Long memberId, String source) {
        BooleanBuilder exerciseFilter = exerciseFilter(exerciseId, source);
        List<Tuple> fetch = getFilteredRecordTuples(exerciseFilter, fetchTopRecordIds(memberId, exerciseFilter));
        return getLocalDateTimeListMap(fetch);
    }

    private List<Tuple> getFilteredRecordTuples(BooleanBuilder exerciseFilter, List<Long> rankRecordIdsQuery) {
        return jpaQueryFactory.select(recordSets, record.startTime)
                .from(recordSets)
                .join(recordDetails).on(recordSets.recordDetails.id.eq(recordDetails.id))
                .join(record).on(recordDetails.record.id.eq(record.id))
                .where(recordDetails.id.in(
                        JPAExpressions.select(recordDetails.id)
                                .from(recordDetails)
                                .where(exerciseFilter.and(recordDetails.record.id.in(rankRecordIdsQuery)))
                )).fetch();
    }

    private List<Long> fetchTopRecordIds(Long memberId, BooleanBuilder exerciseFilter) {
        return jpaQueryFactory
                .select(
                        Expressions.numberTemplate(Integer.class,
                                "row_number() over (order by {0} desc)", record.startTime).as("row_num"),
                        record.id
                )
                .from(record)
                .join(recordDetails).on(recordDetails.record.id.eq(record.id)).fetchJoin()
                .where(exerciseFilter.and(record.member.id.eq(memberId)))
                .limit(7)
                .fetch()
                .stream().map(tuple -> tuple.get(record.id)).toList();
    }

    private static Map<LocalDate, List<RecordSetsResponse>> getLocalDateTimeListMap(List<Tuple> fetch) {
        return fetch.stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(record.startTime).toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                tuple -> RecordSetsResponse.of(tuple.get(recordSets)),
                                Collectors.toList()
                        )
                ));
    }

    private BooleanBuilder exerciseFilter(Long exerciseId, String source) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (source.equals(DEFAULT)) {
            return booleanBuilder.and(recordDetails.exercise.id.eq(exerciseId));
        }
        return booleanBuilder.and(recordDetails.customExercise.id.eq(exerciseId));
    }

    // === findByRecordId() === //
    private List<Tuple> fetchRecordDataWithDetailsAndSets(Long recordId, Long memberId) {
        return jpaQueryFactory.select(record, recordDetails, recordSets).from(record)
                .join(recordDetails).on(record.id.eq(recordDetails.record.id))
                .join(recordSets).on(recordSets.recordDetails.id.eq(recordDetails.id))
                .where(record.id.eq(recordId).and(record.member.id.eq(memberId)))
                .orderBy(recordDetails.orders.asc())
                .fetch();
    }

    private static Map<Record, List<RecordDetailsResponse>> groupRecordsWithDetails(List<Tuple> list) {
        return list.stream()
                .collect(
                        Collectors.groupingBy(
                                tuple -> Objects.requireNonNull(tuple.get(0, Record.class)), mapToDetailsResponses()
                        )
                );
    }

    private static Collector<Tuple, ?, List<RecordDetailsResponse>> mapToDetailsResponses() {
        return Collectors.mapping(
                tuple -> toRecordDetailsResponse(Objects.requireNonNull(tuple.get(1, RecordDetails.class))),
                Collectors.toList()
        );
    }

    private static RecordDetailsResponse toRecordDetailsResponse(RecordDetails recordDetail) {
        return createRecordDetailsResponse(recordDetail, recordDetail.getExercise(), recordDetail.getCustomExercise());
    }

    private static RecordDetailsResponse createRecordDetailsResponse(RecordDetails recordDetail, Exercise exercise,
                                                                     CustomExercise customExercise) {
        return RecordDetailsResponse.of(recordDetail, exercise, customExercise);
    }

    private RecordResponse handleRecordResponse(Map<Record, List<RecordDetailsResponse>> groupedDetails, List<Tuple> list) {
        return groupedDetails
                .entrySet()
                .stream()
                .map(entry -> convertToRecordResponse(entry, list, mapDetailsToIdKeyedResponses(entry.getValue())))
                .findFirst()
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }

    private static Map<Long, RecordDetailsResponse> mapDetailsToIdKeyedResponses(List<RecordDetailsResponse> details) {
        Map<Long, RecordDetailsResponse> detailsMap = new LinkedHashMap<>();
        details.forEach(detail -> detailsMap.put(detail.getRecordDetailId(), detail));
        return detailsMap;
    }

    private RecordResponse convertToRecordResponse(Map.Entry<Record, List<RecordDetailsResponse>> entry,
                                                   List<Tuple> list, Map<Long, RecordDetailsResponse> detailsMap) {
        list.stream()
                .filter(tuple -> tuple.get(1, RecordDetails.class) != null)
                .forEach(tuple -> populateDetailsWithSets(detailsMap, tuple));
        return getRecordResponse(entry.getKey(), detailsMap);
    }

    private static void populateDetailsWithSets(Map<Long, RecordDetailsResponse> detailsMap, Tuple tuple) {
        Long detailId = Objects.requireNonNull(tuple.get(1, RecordDetails.class)).getId();
        RecordSets recordSets = tuple.get(2, RecordSets.class);

        if (detailsMap.containsKey(detailId)) {      // 해당 detailId를 키로 가진 RecordDetailsResponse가 있으면
            RecordDetailsResponse detail = detailsMap.get(detailId);
            List<RecordSetsResponse> sets = updateOrInitializeSets(detail, recordSets);
            detail.setSets(sets);   // 업데이트된 sets 리스트를 RecordDetailsResponse에 설정
        }
    }

    private static List<RecordSetsResponse> updateOrInitializeSets(RecordDetailsResponse detail, RecordSets recordSets) {
        List<RecordSetsResponse> sets = Optional.ofNullable(detail.getSets()).orElseGet(ArrayList::new);
        sets.add(RecordSetsResponse.of(
                Objects.requireNonNull(recordSets).getId(),
                recordSets.getWeight(),
                recordSets.getReps(),
                recordSets.getTimes()
        )); // 현재의 RecordSets 정보를 RecordSetsResponse로 변환하여 sets 리스트에 추가
        return sets;
    }

    private RecordResponse getRecordResponse(Record record, Map<Long, RecordDetailsResponse> detailsMap) {
        return RecordResponse.of(record, detailsMap);
    }

    // === deleteCustomExercise() === //
    private void deleteRecordSetsByCustomExercise(Long customExerciseId) {
        jpaQueryFactory.delete(recordSets)
                .where(recordDetails.id.in(
                        JPAExpressions.select(recordDetails.id)
                                .from(recordDetails)
                                .where(recordDetails.customExercise.id.eq(customExerciseId)))
                ).execute();
    }

    private void deleteRecordDetailsByCustomExercise(Long customExerciseId) {
        jpaQueryFactory
                .delete(recordDetails)
                .where(recordDetails.customExercise.id.eq(customExerciseId))
                .execute();
    }

    // === deleteRecord() === //
    private void deleteRecordSets(Long recordId) {
        jpaQueryFactory.delete(recordSets)
                .where(recordSets.recordDetails.id.in(
                        JPAExpressions.select(recordDetails.id)
                                .from(recordDetails)
                                .where(recordDetails.record.id.eq(recordId)))).execute();
    }

    private void deleteRecordDetails(Long recordId) {
        jpaQueryFactory.delete(recordDetails)
                .where(recordDetails.record.id.eq(recordId))
                .execute();
    }

    private void deleteRecords(Long recordId, Long memberId) {
        jpaQueryFactory.delete(record)
                .where(record.id.eq(recordId).and(record.member.id.eq(memberId)))
                .execute();
    }

    // === getRecordCountAdnDataForMonth() === //
    private List<Record> fetchRecordsByMonth(Long memberId, Integer year, Integer month) {
        return jpaQueryFactory.selectFrom(record)
                .where(record.member.id.eq(memberId)
                        .and(record.endTime
                                .between(getStartOfMonth(year, month), getEndOfMonth(year, month)))
                )
                .orderBy(record.endTime.asc()).fetch();
    }

    private static Map<String, RecordData> mapRecordsByDay(List<Record> records) {
        Map<String, RecordData> dayRecordCount = new LinkedHashMap<>();
        for (Record record : records) {
            String day = String.valueOf(record.getEndTime().getDayOfMonth());
            RecordData recordData = dayRecordCount.getOrDefault(day, new RecordData());
            List<RecordDay> recordDays = addRecordDayToList(recordData, RecordDay.of(record));
            recordData.setDays(record, recordDays);
            dayRecordCount.put(day, recordData);
        }
        return dayRecordCount;
    }

    private static List<RecordDay> addRecordDayToList(RecordData recordData, RecordDay recordDay) {
        List<RecordDay> recordDays = Optional.ofNullable(recordData.getRecordDays()).orElseGet(ArrayList::new);
        recordDays.add(recordDay);
        return recordDays;
    }

    // === getRecordPage() === //
    private List<Record> fetchRecordsForMonth(Long memberId, Integer year, Integer month) {
        return jpaQueryFactory.selectFrom(record)
                .where(record.member.id.eq(memberId).and(record.endTime
                        .between(getStartOfMonth(year, month), getEndOfMonth(year, month))))
                .orderBy(record.endTime.desc()).fetch();
    }

    private static List<RecordDay> mapToRecordDays(List<Record> records) {
        return records.stream().map(RecordDay::recordPage_Of).collect(Collectors.toList());
    }

}
