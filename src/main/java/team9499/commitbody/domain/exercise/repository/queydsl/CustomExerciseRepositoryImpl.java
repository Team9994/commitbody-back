package team9499.commitbody.domain.exercise.repository.queydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.domain.exercise.dto.ReportDto;
import team9499.commitbody.domain.exercise.dto.WeekReport;

import java.util.*;

import static team9499.commitbody.domain.exercise.dto.ReportDto.*;
import static team9499.commitbody.domain.record.domain.QRecord.*;
import static team9499.commitbody.domain.record.domain.QRecordDetails.*;
import static team9499.commitbody.domain.record.domain.QRecordSets.*;
import static team9499.commitbody.global.utils.TimeConverter.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomExerciseRepositoryImpl implements CustomExerciseRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public ReportDto getWeeklyExerciseVolumeReport(Long memberId, Long exerciseId, ExerciseType exerciseType) {
        ReportDtoBuilder report = getReport(exerciseId, memberId, exerciseType);
        return getWeeklyReport(exerciseId, memberId, exerciseType, report);
    }

    private ReportDtoBuilder getReport(Long exerciseId, Long memberId, ExerciseType exerciseType) {
        if (isWeightAndRep(exerciseType)) {
            return getVolumeSummary(exerciseId, memberId);
        } else if (isRep(exerciseType)) {
            return getRepsSummary(exerciseId, memberId);
        }
        return getTimeSummary(exerciseId, memberId);
    }

    private ReportDtoBuilder getVolumeSummary(Long exerciseId, Long memberId) {
        Tuple tuple = jpaQueryFactory.select(recordDetails.max1RM.max(), recordDetails.detailsVolume.sum())
                .from(record)
                .join(recordDetails).on(recordDetails.record.id.eq(record.id))
                .where(recordDetails.exercise.id.eq(exerciseId).and(record.member.id.eq(memberId)))
                .fetchOne();
        return weightOf(tuple.get(recordDetails.max1RM.max()), tuple.get(recordDetails.detailsVolume.sum()));
    }

    private ReportDtoBuilder getRepsSummary(Long exerciseId, Long memberId) {
        Tuple tuple = jpaQueryFactory.select(recordDetails.maxReps.max(), recordDetails.detailsReps.sum())
                .from(record)
                .join(recordDetails).on(recordDetails.record.id.eq(record.id))
                .where(recordDetails.exercise.id.eq(exerciseId).and(record.member.id.eq(memberId)))
                .fetchOne();

        return repOf(tuple.get(recordDetails.maxReps.max()), tuple.get(recordDetails.detailsReps.sum()));
    }

    private ReportDtoBuilder getTimeSummary(Long exerciseId, Long memberId) {
        Tuple tuple = jpaQueryFactory.select(recordSets.times.max(), recordDetails.sumTimes.sum())
                .from(record)
                .join(recordDetails).on(recordDetails.record.id.eq(record.id))
                .join(recordSets).on(recordSets.recordDetails.id.eq(recordDetails.id))
                .where(recordDetails.exercise.id.eq(exerciseId).and(record.member.id.eq(memberId)))
                .fetchOne();

        return timeOf(tuple.get(recordSets.times.max()), tuple.get(recordDetails.sumTimes.sum()));
    }


    private ReportDto getWeeklyReport(Long exerciseId, Long memberId, ExerciseType exerciseType, ReportDtoBuilder reportDtoBuilder) {
        NumberPath<Integer> metricPath = resolveDetailsMetric(exerciseType);
        List<Tuple> tuples = getTuples(exerciseId, memberId, metricPath);

        return buildReport(reportDtoBuilder, exerciseType, getWeekReports(tuples), calculateMetricSum(tuples, metricPath));
    }

    private static NumberPath<Integer> resolveDetailsMetric(ExerciseType exerciseType) {
        if (isWeightAndRep(exerciseType)) {
            return recordDetails.detailsVolume;
        }
        if (isRep(exerciseType)) {
            return recordDetails.detailsReps;
        }
        return recordDetails.sumTimes;
    }

    private List<Tuple> getTuples(Long exerciseId, Long memberId, NumberPath<Integer> metricPath) {
        return jpaQueryFactory.select(metricPath, record)
                .from(record)
                .join(recordDetails).on(recordDetails.record.id.eq(record.id)).fetchJoin()
                .where(record.member.id.eq(memberId)
                        .and(recordDetails.exercise.id.eq(exerciseId))
                        .and(record.startTime.between(startOfWeek(), endOfWeek()))
                        .and(record.member.isWithdrawn.eq(false)))
                .fetch();
    }

    private static List<WeekReport> getWeekReports(List<Tuple> tuples) {
        return tuples.stream()
                .map(tuple -> WeekReport.of(tuple.get(record)))
                .toList();
    }

    private int calculateMetricSum(List<Tuple> fetch, NumberPath<Integer> metricPath) {
        return fetch.stream()
                .mapToInt(tuple -> {
                    Integer value = tuple.get(metricPath);
                    return value != null ? value : 0;
                })
                .sum();
    }

    private ReportDto buildReport(ReportDtoBuilder reportDtoBuilder, ExerciseType exerciseType, List<WeekReport> list, int sum) {
        if (isWeightAndRep(exerciseType)) {
            return reportDtoBuilder.weekReports(list).weekVolume(sum).build();
        } else if (isRep(exerciseType)) {
            return reportDtoBuilder.weekReports(list).weekRep(sum).build();
        }
        return reportDtoBuilder.weekReports(list).weekTime(sum).build();
    }

    private static boolean isRep(ExerciseType exerciseType) {
        return exerciseType.equals(ExerciseType.REPS_ONLY);
    }

    private static boolean isWeightAndRep(ExerciseType exerciseType) {
        return exerciseType.equals(ExerciseType.WEIGHT_AND_REPS);
    }

}