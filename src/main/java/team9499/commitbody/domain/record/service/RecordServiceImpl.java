package team9499.commitbody.domain.record.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.record.domain.Record;
import team9499.commitbody.domain.record.domain.RecordDetails;
import team9499.commitbody.domain.record.domain.RecordSets;
import team9499.commitbody.domain.record.dto.RecordStatistics;
import team9499.commitbody.domain.record.dto.RecordDetailsDto;
import team9499.commitbody.domain.record.dto.RecordDto;
import team9499.commitbody.domain.record.dto.RecordSetsDto;
import team9499.commitbody.domain.record.dto.response.RecordMonthResponse;
import team9499.commitbody.domain.record.dto.response.RecordResponse;
import team9499.commitbody.domain.record.repository.RecordDetailsRepository;
import team9499.commitbody.domain.record.repository.RecordRepository;
import team9499.commitbody.domain.record.repository.RecordSetsRepository;
import team9499.commitbody.global.Exception.*;
import team9499.commitbody.global.redis.RedisService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static team9499.commitbody.domain.record.dto.response.RecordMonthResponse.*;
import static team9499.commitbody.global.Exception.ExceptionStatus.*;
import static team9499.commitbody.global.Exception.ExceptionType.*;
import static team9499.commitbody.global.constants.ElasticFiled.*;

@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final RecordDetailsRepository recordDetailsRepository;
    private final RecordSetsRepository recordSetsRepository;
    private final ExerciseRepository exerciseRepository;
    private final CustomExerciseRepository customExerciseRepository;
    private final RecordBatchService recordBatchService;
    private final RedisService redisService;

    /**
     * 루틴 완료시 루틴의 대한기록을 저장하는 메서드
     */
    @Override
    public Long saveRecord(Long memberId, String recordName, LocalDateTime startTime, LocalDateTime endTime, List<RecordDto> recordDtos) {
        Member member = getMember(memberId);
        RecordStatistics recordStatistics = RecordStatistics.init();
        Record record = Record.create(recordName, startTime, endTime, getExerciseDuration(startTime, endTime), member);
        List<RecordSets> recordSets = new ArrayList<>();
        List<RecordDetails> recordDetails = getRecordDetails(recordDtos, recordStatistics, record, recordSets, member);
        record.setRecord(recordStatistics);
        return handleSaveRecord(record, recordDetails, recordSets);
    }

    @Override
    public RecordResponse getRecord(Long recordId, Long memberId) {
        return recordRepository.findByRecordId(recordId, memberId);
    }

    /**
     * 기록 수정시 한번 기록의 관련된 데이터를 일괄(배치) 삭제후 새롭게 저장
     * 기록 명은 변경시에만 업데이트
     */
    @Override
    public void updateRecord(Long memberId, Long recordId, String recordName, List<RecordDto> recordDtos) {
        Record record = recordRepository.findByIdAndMemberId(recordId, memberId);
        Member member = getMember(memberId);
        updateRecordName(recordName, record);
        List<Long> deleteDetailsIds = getDeleteDetailsIDs(record);
        RecordStatistics recordStatistics = RecordStatistics.init();
        List<RecordSets> recordSets = new ArrayList<>();
        List<RecordDetails> recordDetails = getRecordDetails(recordDtos, recordStatistics, record, recordSets, member);
        handleUpdateRecord(recordDetails, recordSets, record, recordStatistics, deleteDetailsIds);
    }

    /**
     * 기록 삭제 - 작성자만이 기록을 삭제 가능하다.
     */
    @Override
    public void deleteRecord(Long memberId, Long recordId) {
        Record record = recordRepository.findByIdAndMemberId(recordId, memberId);
        validWriter(record);
        recordRepository.deleteRecord(recordId, memberId);
    }

    /**
     * 해당 사용자의 운동 기록을 달력, 모든 데이터를 조회하여 RecordMonthResponse 적재
     */
    @Transactional(readOnly = true)
    @Override
    public RecordMonthResponse getRecordForMember(Long memberId, Integer year, Integer month) {
        Map<String, RecordData> recordCountAdnDataForMonth = recordRepository.getRecordCountAdnDataForMonth(memberId, year, month);  //일별 기록
        List<RecordDay> recordPage = recordRepository.getRecordPage(memberId, year, month);// 해당달 전제 데이터
        return new RecordMonthResponse(recordCountAdnDataForMonth, recordPage);

    }

    private static int getExerciseDuration(LocalDateTime startTime, LocalDateTime endTime) {
        return (int) Duration.between(startTime, endTime).toMinutes();
    }

    private List<RecordDetails> getRecordDetails(List<RecordDto> recordDtos, RecordStatistics recordStatistics,
                                                 Record record, List<RecordSets> recordSets, Member member) {
        List<RecordDetails> recordDetails = new ArrayList<>();
        processExerciseRoutines(recordDtos, recordStatistics, record, recordSets, recordDetails, member);
        return recordDetails;
    }

    private void processExerciseRoutines(List<RecordDto> recordDtos, RecordStatistics recordStatistics, Record record,
                                         List<RecordSets> recordSets, List<RecordDetails> recordDetails, Member member) {
        int orders = 1;
        for (RecordDto recordDto : recordDtos) {        // 실행한 운동 루틴 순회
            recordStatistics.exerciseSizePlus();
            RecordDetails recordDetail = RecordDetails.create(getExercise(recordDto), record, orders++);       //운동이 적용된 상세 루턴 객체 생성
            RecordDetailsDto detailsDto = RecordDetailsDto.init();
            processExerciseSets(recordDto, recordStatistics, detailsDto, recordSets, recordDetail, member);
            recordDetail.setDetailsSets(detailsDto.getDetailsSets());
            updateRecordDetails(detailsDto, recordDetail);
            recordDetails.add(recordDetail);
        }
    }

    private Object getExercise(RecordDto recordDto) {
        Long exerciseId = recordDto.getExerciseId();
        if (recordDto.getSource().equals(DEFAULT)) { // 기본 운동일때
            return getExercise(exerciseId);
        }
        return getCustomExercise(exerciseId);
    }

    private static void processExerciseSets(RecordDto recordDto, RecordStatistics recordStatistics, RecordDetailsDto detailsDto,
                                            List<RecordSets> recordSets, RecordDetails recordDetail, Member member) {
        for (RecordSetsDto set : recordDto.getSets()) {         // 운동별 기록을 저장하기 위해 순회
            Integer reps = set.getReps();       // 세트당 진횅 횟수
            Integer weight = set.getWeight();       // 세트당 무게
            Integer times = set.getTimes();         // 운동 시간
            validZero(weight, reps, times);
            recordStatistics.totalSetPlus();
            detailsDto.detailSetsPlus();
            addRecordSetsAndUpdateDetails(recordStatistics, detailsDto, recordSets, recordDetail, weight, reps, times, member);
        }
    }

    private static void addRecordSetsAndUpdateDetails(RecordStatistics recordStatistics, RecordDetailsDto detailsDto,
                                                      List<RecordSets> recordSets, RecordDetails recordDetail,
                                                      Integer weight, Integer reps, Integer times, Member member) {
        if (weight != null && reps != null) {
            handleWeightAndReps(recordStatistics, detailsDto, recordSets, recordDetail, weight, reps, member);
            return;
        }
        if (times != null) {
            handleTimes(recordStatistics, detailsDto, recordSets, recordDetail, times, member);
            return;
        }
        handleOnlyReps(recordStatistics, detailsDto, recordSets, recordDetail, reps, member);
    }

    private static void handleWeightAndReps(RecordStatistics recordStatistics, RecordDetailsDto detailsDto, List<RecordSets> recordSets, RecordDetails recordDetail, Integer weight, Integer reps, Member member) {
        recordSets.add(RecordSets.ofWeightAndSets(weight, reps, recordDetail));
        recordStatistics.totalVolumePlus(weight);
        recordStatistics.caloriesPlus(calculateTotalCalorie(member, getMets(recordDetail)));
        detailsDto.setWeightAndReps(weight, reps);
    }

    private static void handleTimes(RecordStatistics recordStatistics, RecordDetailsDto detailsDto, List<RecordSets> recordSets, RecordDetails recordDetail, Integer times, Member member) {
        recordSets.add(RecordSets.ofTimes(times, recordDetail));
        detailsDto.setTimes(times);
        recordStatistics.caloriesPlus(calculateTotalCalorieTimes(member, getMets(recordDetail), times));
    }

    private static void handleOnlyReps(RecordStatistics recordStatistics, RecordDetailsDto detailsDto, List<RecordSets> recordSets, RecordDetails recordDetail, Integer reps, Member member) {
        recordStatistics.caloriesPlus(calculateTotalCalorie(member, getMets(recordDetail)));
        recordSets.add(RecordSets.ofSets(reps, recordDetail));
        detailsDto.setReps(detailsDto.getMaxReps(), reps);
    }

    private static float getMets(RecordDetails recordDetail) {
        if (recordDetail.getExercise() != null) {
            return recordDetail.getExercise().getMets();
        }
        return 4;
    }

    private static void updateRecordDetails(RecordDetailsDto detailsDto, RecordDetails recordDetail) {
        if (detailsDto.isWeightValid()) {       // 무게 기준일 때
            recordDetail.setWeight(detailsDto);
            return;
        }
        if (detailsDto.isTimesValid()) {    // 시간 기준일 때
            recordDetail.setSumTimes(detailsDto.getDetailsTime());
            return;
        }             // 횟수 기준일 때
        recordDetail.setReps(detailsDto);
    }

    private Long handleSaveRecord(Record record, List<RecordDetails> recordDetails, List<RecordSets> recordSets) {
        Record save = recordRepository.save(record);
        recordDetailsRepository.saveAll(recordDetails);
        recordSetsRepository.saveAll(recordSets);
        return save.getId();
    }

    private Member getMember(Long memberId) {
        return redisService.getMemberDto(memberId.toString()).get();
    }


    private Exercise getExercise(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }

    private CustomExercise getCustomExercise(Long exerciseId) {
        return customExerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }

    private static int calculateTotalCalorie(Member member, float mets) {
        return (int) (member.getWeight() * mets * 1.0f / 60.0f);
    }

    private static int calculateTotalCalorieTimes(Member member, float mets, int time) {
        return (int) (member.getWeight() * mets * time / 60.0f);
    }


    private static void updateRecordName(String recordName, Record record) {
        if (!record.getRecordName().equals(recordName)) {
            record.updateRecordName(recordName);
        }
    }

    private static List<Long> getDeleteDetailsIDs(Record record) {
        return record.getDetailsList().stream().map(RecordDetails::getId).collect(Collectors.toList());
    }

    private void handleUpdateRecord(List<RecordDetails> recordDetails, List<RecordSets> recordSets, Record record, RecordStatistics recordStatistics, List<Long> deleteDetailsIds) {
        recordDetailsRepository.saveAll(recordDetails);
        recordBatchService.insertSetsInBatch(recordSets);      // 배치를 통한 대량 저장
        record.updateRecord(recordStatistics.getTotalVolume(), recordStatistics.getCalories(), recordStatistics.getTotalSets()); // 최종 기록 도메인 데이터 수정
        recordBatchService.deleteDetailsIdsInBatch(deleteDetailsIds);   // 비동기로 삭제
    }

    /*
     sets 의 0 값이 들어오면 예외 발생
     */
    private static void validZero(Integer weight, Integer reps, Integer times) {
        if (weight != null && weight == 0) {
            throw new InvalidUsageException(BAD_REQUEST, NOT_USE_ZERO);
        }
        if (reps != null && reps == 0) {
            throw new InvalidUsageException(BAD_REQUEST, NOT_USE_ZERO);
        }
        if (times != null && times == 0) {
            throw new InvalidUsageException(BAD_REQUEST, NOT_USE_ZERO);
        }
    }

    private static void validWriter(Record record) {
        if (record == null) throw new InvalidUsageException(FORBIDDEN, AUTHOR_ONLY);
    }

}
