package team9499.commitbody.domain.exercise.repository.queydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.exercise.domain.*;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.domain.exercise.dto.response.ExerciseDetailsResponse;
import team9499.commitbody.domain.exercise.dto.response.ExerciseResponse;
import team9499.commitbody.domain.record.domain.Record;
import team9499.commitbody.domain.record.domain.RecordDetails;
import team9499.commitbody.domain.record.domain.RecordSets;
import team9499.commitbody.domain.record.dto.response.RecordSetsResponse;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static team9499.commitbody.domain.Member.domain.QMember.*;
import static team9499.commitbody.domain.exercise.domain.QCustomExercise.*;
import static team9499.commitbody.domain.exercise.domain.QExercise.*;
import static team9499.commitbody.domain.record.domain.QRecord.*;
import static team9499.commitbody.domain.record.domain.QRecordDetails.*;
import static team9499.commitbody.domain.record.domain.QRecordSets.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomExerciseRepositoryImpl implements CustomExerciseRepository{

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * 운동 상세페이지에서 운동의 대한 기록, 통계를 내리는 메서드
     * @param memberId  로그인한 사용자 ID
     * @param exerciseId    검색할 운동 ID
     * @param source    운동의 대한 종류[default, custom]
     * @return  조회된 ExerciseResponse 클래스 반환
     */
    @Override
    public ExerciseResponse getExerciseDetailReport(Long memberId, Long exerciseId, String source) {
        // 제공된 운동인지 커스텀 운동인지 동적 쿼리 생성
        BooleanBuilder builder = getBooleanBuilder(exerciseId, source);

        // 오늘 날짜 기준으로 주의 시작과 끝 계산
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDateTime startOfWeek = weekStart.atStartOfDay();
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        LocalDateTime endOfWeek = weekEnd.atTime(23, 59, 59);;

        // 쿼리 생성
        List<Tuple> tupleList = jpaQueryFactory
                .select(record, recordSets, recordDetails)
                .from(record)
                .join(member).on(member.id.eq(record.member.id))
                .join(recordDetails).on(recordDetails.record.id.eq(record.id))
                .join(recordSets).on(recordSets.recordDetails.id.eq(recordDetails.id))
                .where(builder.and(record.member.id.eq(memberId)).and(record.startTime.between(startOfWeek, endOfWeek)))
                .fetch();

        // 제공된 운동인지 커스텀 운동인지 동적 쿼리 생성
        BooleanBuilder newBooleanBuilder = getBooleanBuilder(exerciseId, source);

        // 사용자가 기록한 운동 데이터 모두 조회
        List<RecordSets> countQuery = jpaQueryFactory.select(recordSets).from(record)
                .join(member).on(member.id.eq(record.member.id))
                .join(recordDetails).on(recordDetails.record.id.eq(record.id))
                .join(recordSets).on(recordSets.recordDetails.id.eq(recordDetails.id))
                .where(newBooleanBuilder.and(record.member.id.eq(memberId))).fetch();

        int maxValue = 0;       // 운동의 최댓 값
        int totalValue = 0;     // 운동데이터의 총 합

        for (var value : countQuery) {
            int sum = 0;
            if (value.getReps() != null && value.getWeight() != null) {         // 무게+횟수 기반
                sum = value.getWeight() * value.getReps();
                maxValue = Math.max(maxValue, value.getWeight());
            } else if (value.getTimes() != null) {                              // 시간 기반
                sum = value.getTimes();
                maxValue = Math.max(maxValue, value.getTimes());
            } else if (value.getReps() != null) {                               // 횟수 기반
                sum = value.getReps();
                maxValue = Math.max(maxValue, value.getReps());
            }
            totalValue += sum;
        }


        // 첫번째 운동 기록을 조회
        RecordDetails details = countQuery.isEmpty() ? null : countQuery.get(0).getRecordDetails();

        // 만약 details가 null이라면, source에 따라 적절한 Exercise 또는 CustomExercise를 가져와서 RecordDetails 생성
        if (details == null) {
            if (source.equals("default")) {
                Exercise exercise1 = jpaQueryFactory.select(exercise).from(exercise).where(exercise.id.eq(exerciseId)).fetchOne();
                details = new RecordDetails().onlyExercise(exercise1);
            } else {
                CustomExercise customExercise1 = jpaQueryFactory.selectFrom(customExercise).where(customExercise.id.eq(exerciseId)).fetchOne();
                details = new RecordDetails().onlyExercise(customExercise1);
            }
        }

        // 운동 상세 정보에서 적용된 운동이 뭔지 찾아 Object로 반환
        Object exercise = details.getExercise() != null ? details.getExercise() : details.getCustomExercise();

        // 요일별 ExerciseDayRecord를 저장할 Map
        Map<DayOfWeek, Integer> dayRecordMap = new HashMap<>();

        // 한주의 운동 데이터
        Integer weekValue = 0;
        // Record를 기준으로 요일별 ExerciseDayRecord 집계
        for (Record record : tupleList.stream().map(tuple -> tuple.get(record)).collect(Collectors.toSet())) {
            LocalDateTime endTime = record.getEndTime();
            DayOfWeek dayOfWeek = LocalDate.of(endTime.getYear(), endTime.getMonth(), endTime.getDayOfMonth()).getDayOfWeek();

            RecordDetails rd = tupleList.stream()
                    .map(tuple -> tuple.get(recordDetails))
                    .filter(recordDetails1 -> recordDetails1.getRecord().getId().equals(record.getId()))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));

            
            //운동 타입의 종류 검사
            boolean exerciseType = exerciseType(details.getExercise());

            Integer records = 0;

            // 기본운동일 경우 true, 커스텀 운동 false
            if (exerciseType) {
                Exercise exercise1 = details.getExercise();
                ExerciseType exerciseType1 = exercise1.getExerciseType();
                
                // 레코드 세트에서 레코드 기록이랑 같은Id값을 리스트로 조회
                List<RecordSets> collect = tupleList.stream()
                        .map(tuple -> tuple.get(recordSets))
                        .filter(recordSets1 -> recordSets1.getRecordDetails().getId().equals(rd.getId()))
                        .collect(Collectors.toList());

                if (exerciseType1.equals(ExerciseType.REPS_ONLY)) {         // 횟수기반
                    records += collect.stream().mapToInt(RecordSets::getReps).sum();
                    weekValue += collect.stream().mapToInt(RecordSets::getReps).sum();
                } else if (exerciseType1.equals(ExerciseType.WEIGHT_AND_REPS)) {            // 무게 + 횟수 기반
                    records += collect.stream().mapToInt(RecordSets::getWeight).sum();
                    weekValue += collect.stream().mapToInt(RecordSets::getWeight).sum();
                } else {                                                                   // 시간 기반
                    records += collect.stream().mapToInt(RecordSets::getTimes).sum();
                    weekValue += collect.stream().mapToInt(RecordSets::getTimes).sum();
                }
                dayRecordMap.put(dayOfWeek,records);
            }
        }





        // ExerciseDetailsResponse 집계
        List<ExerciseDetailsResponse> allDetailsResponses = tupleList.stream()
                // `tupleList`를 `record`를 기준으로 그룹화하고, 각 그룹을 `ExerciseDetailsResponse` 객체 리스트로 매핑
                .collect(Collectors.groupingBy(tuple -> tuple.get(record), Collectors.mapping(
                        tuple -> {
                            // 각 `tuple`에서 `record`와 `recordDetails`를 추출
                            Record record1 = tuple.get(record);
                            RecordDetails recordDetail = tuple.get(recordDetails);
//                            log.info("Processing record ID: {} with EndTime: {}", record1.getId(), record1.getEndTime());

                            // `ExerciseDetailsResponse` 객체 생성
                            return ExerciseDetailsResponse.of(
                                    record1.getId(),
                                    record1.getEndTime(),
                                    recordDetail.getId(),
                                    new ArrayList<>()
                            );
                        },
                        // 동일한 `record`를 가지는 `tuple`을 리스트
                        Collectors.toList()
                )))
                .entrySet()  // 그룹화된 결과의 엔트리 집합을 스트림으로 변환
                .stream()
                .flatMap(entry -> {     // 각 그룹의 값을 일자로 스트림으로 변환
                    // 현재 그룹의 값을 `detailId`를 기준으로 맵으로 변환
                    Map<Long, ExerciseDetailsResponse> detailsMap = entry.getValue().stream()
                            .collect(Collectors.toMap(ExerciseDetailsResponse::getRecordDetailId, e -> e, (existing, replacement) -> existing));

                    // 원래 `tupleList`에서 `recordDetails`가 있는 항목만 필터링
                    tupleList.stream()
                            .filter(tuple -> tuple.get(recordDetails) != null)
                            .forEach(tuple -> {
                                Long detailId = tuple.get(recordDetails).getId();
                                RecordSets recordSets1 = tuple.get(recordSets);

                                // `detailsMap`에 해당 `detailId`가 있으면 세트 리스트에 추가
                                if (detailsMap.containsKey(detailId)) {
                                    ExerciseDetailsResponse exerciseDetailsResponse = detailsMap.get(detailId);
                                    List<RecordSetsResponse> sets = Optional.ofNullable(exerciseDetailsResponse.getSets()).orElse(new ArrayList<>());
                                    sets.add(RecordSetsResponse.of(
                                            recordSets1.getWeight(),
                                            recordSets1.getReps(),
                                            recordSets1.getTimes()
                                    ));
                                    exerciseDetailsResponse.setSets(sets);
                                }
                            });

                    // 업데이트된 `detailsMap`의 값들(즉, `ExerciseDetailsResponse` 객체들)을 스트림으로 변환하여 반환
                    return detailsMap.values().stream();
                })
                .sorted(Comparator.comparing(ExerciseDetailsResponse::getDate).reversed()) // 최신순으로 정렬
                .limit(7)
                .collect(Collectors.toList()); // 결과의 데이터를 리스트로 반환

        // ExerciseResponse 생성 및 반환
        Set<String> exerciseMethods = getMethodList(exercise);
        return ExerciseResponse.of(exercise,maxValue,totalValue,weekValue ,dayRecordMap, exerciseMethods, allDetailsResponses);
    }

    /*
    운동이 어떤 운동인지 판별후 동적 쿼리를 만들어주는 메서드
     */
    private static BooleanBuilder getBooleanBuilder(Long exerciseId, String source) {
        BooleanBuilder builder = new BooleanBuilder();
        if (source.equals("default")) {
            builder.and(recordDetails.exercise.id.eq(exerciseId));
        } else {
            builder.and(recordDetails.customExercise.id.eq(exerciseId));
        }
        return builder;
    }

    /*
    운동 순서를 Set으로 반환
     */
    private static Set<String> getMethodList(Object exercise) {
        Set<String> stringList = new LinkedHashSet<>();
        if (exercise instanceof Exercise) {
            List<ExerciseMethod> exerciseMethodList = ((Exercise) exercise).getExerciseMethodList();
            for (ExerciseMethod exerciseMethod : exerciseMethodList) {
                stringList.add(exerciseMethod.getExercise_content());
            }
        }
        return stringList;
    }

    /*
    운동의 종류를 체크 [일반 제공운동 : default, 커스텀 운동 : custom]
     */
    private boolean exerciseType(Object exercise) {
        return exercise instanceof Exercise;
    }
}