package team9499.commitbody.domain.record.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.record.domain.Record;
import team9499.commitbody.domain.record.domain.RecordDetails;
import team9499.commitbody.domain.record.domain.RecordSets;
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

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RecordServiceImpl implements RecordService{

    private final RecordRepository recordRepository;
    private final RecordDetailsRepository recordDetailsRepository;
    private final RecordSetsRepository recordSetsRepository;
    private final ExerciseRepository exerciseRepository;
    private final CustomExerciseRepository customExerciseRepository;
    private final MemberRepository memberRepository;
    private final RecordBatchService recordBatchService;
    private final RedisService redisService;

    private final EntityManager em;

    private final String DEFAULT ="default";

    // TODO: 2024-08-14 리팩토링 필요
    /**
     * 루틴 완료시 루틴의 대한기록을 저장하는 메서드
     * @param memberId 사용자 ID
     * @param recordName 기록명
     * @param startTime 시작 시간
     * @param endTime 마무리 시간
     */
    @Override
    public Long saveRecord(Long memberId, String recordName, LocalDateTime startTime, LocalDateTime endTime, List<RecordDto> recordDtos) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, No_SUCH_MEMBER));
        int totalVolume = 0;        // 투틴 총 수행 볼륨
        int totalSets = 0;          // 루틴 총 진행 세트
        int exerciseSize = 0;       // 루틴의 총 운동수
        float totalMets = 0;        // 총 Mets값

        int exerciseDuration = (int) Duration.between(startTime, endTime).toMinutes();

        Record record = Record.create(recordName, startTime, endTime, exerciseDuration, member);

        List<RecordSets> recordSets = new ArrayList<>();
        List<RecordDetails> recordDetails = new ArrayList<>();

        int orders = 1;
        for (RecordDto recordDto : recordDtos) {        // 실행한 운동 루틴 순회
            exerciseSize++;
            String source = recordDto.getSource();      // 운동 타입 조회 [default, custom]
            Long exerciseId = recordDto.getExerciseId();    // 운동 ID
            Object exercise;
            if (source.equals("default")){ // 기본 운동일때
                Exercise defualt_exercise = exerciseRepository.findById(exerciseId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, NO_SUCH_DATA));
                totalMets+= defualt_exercise.getMets();
                exercise = defualt_exercise;
            }else {                       // 커스텀 운동일때
                exercise = customExerciseRepository.findById(exerciseId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, NO_SUCH_DATA));
                totalMets+= 4;
            }

            RecordDetails recordDetail = RecordDetails.create(exercise,record,orders++);       //운동이 적용된 상세 루턴 객체 생성

            int detailsVolume = 0;      // 상세 운동별 총 볼륨
            int detailsSets = 0;        // 상세 운동별 총 세트
            int detailsReps = 0;        // 상세 운도별 총 횟수
            int detailsTime = 0;        // 상세 운동별 수행 시간
            int maxRm = 0;              // 최대 1rm
            int weightCount=0;
            int maxReps = 0;
            boolean weightValid = false;
            boolean timesValid = false;

            for (team9499.commitbody.domain.record.dto.RecordSetsDto set : recordDto.getSets()) {         // 운동별 기록을 저장하기 위해 순회
                Integer reps = set.getReps();       // 세트당 진횅 횟수
                Integer weight = set.getWeight();       // 세트당 무게
                Integer times = set.getTimes();         // 운동 시간
                totalSets += 1;
                detailsSets += 1;
                if (weight !=null && reps!=null){       // 무게+횟수일때
                    recordSets.add(RecordSets.ofWeightAndSets(weight,reps,recordDetail));
                    totalVolume += weight;
                    detailsVolume += (weight*reps);
                    detailsReps += reps;
                    weightValid =true;
                    maxRm += Math.round((float)weight * (float)(1 +0.03333*reps));      // 칼로리 계산
                    weightCount++;
                } else if (times != null) {     // 시간일떄
                    recordSets.add(RecordSets.ofTimes(times,reps,recordDetail));
                    detailsTime += times;
                    timesValid = true;
                }else{                  // 횟수만 사용할떄
                    recordSets.add(RecordSets.ofSets(reps,recordDetail));
                    maxReps = Math.max(maxReps,reps);
                    detailsReps+=reps;
                }
            }
            recordDetail.setDetailsSets(detailsSets);

            if (weightValid){       // 무게 기준일 때
                recordDetail.setDetailsVolume(detailsVolume);
                recordDetail.setDetailsReps(detailsReps);
                recordDetail.setMax1RM(maxRm/weightCount);       // 1RM 계산
            } else if (timesValid) {    // 시간 기준일 때
                recordDetail.setSumTimes(detailsTime);
            }else {             // 횟수 기준일 때
                recordDetail.setDetailsReps(detailsReps);
                recordDetail.setMaxReps(maxReps);
            }

            recordDetails.add(recordDetail);
        }
        int totalCalorie = calculateTotalCalorie(startTime, endTime, member, exerciseSize, totalMets);

        record.setRecordSets(totalSets);
        record.setRecordVolume(totalVolume);;
        record.setRecordCalorie(totalCalorie);

        Record save = recordRepository.save(record);
        recordDetailsRepository.saveAll(recordDetails);
        recordSetsRepository.saveAll(recordSets);
        return save.getId();
    }

    @Transactional(readOnly = true)
    @Override
    public RecordResponse getRecord(Long recordId,Long memberId) {
        return recordRepository.findByRecordId(recordId,memberId);
    }

    /**
     * 기록 수정시 한번 기록의 관련된 데이터를 일괄(배치) 삭제후 새롭게 저장
     * 기록 명은 변경시에만 업데이트
     * @param memberId 로그인한 사용자 ID
     * @param recordId  수정할 기록 ID
     * @param recordName 변경할 기록명
     * @param recordDtos 변경할 기록 데이터 리스트
     */
    @Override
    public void updateRecord(Long memberId, Long recordId, String recordName , List<RecordDto> recordDtos) {
        Record record = recordRepository.findByIdAndMemberId(recordId, memberId);

        Member member = redisService.getMemberDto(String.valueOf(memberId)).get();

        // 만약 레코드 명이 변경됬다면 변경
        if (!record.getRecordName().equals(recordName)){
            record.updateRecordName(recordName);
        }

        List<Long> deleteDetailsIds = record.getDetailsList().stream().map(RecordDetails::getId).collect(Collectors.toList());// 삭제할 디테일 루틴 id 리스트

        List<RecordDetails> newDetails = new ArrayList<>();
        List<RecordSets> newSets = new ArrayList<>();

        int recordSets = 0;
        int exerciseSize = 0;
        int recordVolume = 0;
        float totalMets = 0;

        for (RecordDto recordDto : recordDtos) {
            recordSets++;
            exerciseSize++;

            Object exercise;
            if ("default".equals(recordDto.getSource())) {
                Exercise defaultExercise = getExercise(recordDto.getExerciseId());
                exercise = defaultExercise;
                totalMets   += defaultExercise.getMets();
            } else {
                exercise = getCustomExercise(recordDto.getExerciseId());
                totalMets   += 4;
            }

            RecordDetails recordDetails = RecordDetails.create(exercise, record, recordDto.getOrders());

            Integer max1Rm = 0; // 최대 1RM
            int count = 0; // 무게+횟수 기록 운동 갯수
            Integer totalTimes = 0; // 총 운동 시간(시간일때)
            int maxReps = 0; // 총 운동 횟수
            int totalVolume = 0; // 총 볼륨
            int totalSets = 0; // 총 진행한 세트수
            int totalReps = 0; // 총 진행한 횟수

            for (RecordSetsDto recordSetsDto : recordDto.getSets()) {
                Integer reps = recordSetsDto.getReps();
                Integer weight = recordSetsDto.getWeight();
                Integer times = recordSetsDto.getTimes();
                totalSets++; //세트수 증가

                if (reps != null && weight != null) { // 무게 + 횟수일때
                    newSets.add(RecordSets.ofWeightAndSets(weight, reps, recordDetails));
                    max1Rm += calculate1RM(weight, reps); // 1RM 계산
                    totalVolume += reps * weight; // 총 볼륨 계산
                    recordVolume += reps * weight; // 기록 볼륨 누적
                    totalReps += reps; // 총 횟수 계산
                    count++;
                } else if (times != null) { // 시간일때
                    newSets.add(RecordSets.ofTimes(times, reps, recordDetails));
                    totalTimes += times; // 총 시간 누적
                } else if (reps != null) { // 횟수만 있을때
                    newSets.add(RecordSets.ofSets(reps, recordDetails));
                    maxReps = Math.max(reps, maxReps); // 최대 횟수 갱신
                    totalReps += reps; // 총 횟수 계산
                }
            }

            recordDetails.setDetailsSets(totalSets); // 총 세트수 설정

            if (count > 0 && max1Rm != 0) { // 무게 + 횟수 있을때
                recordDetails.setMax1RM(max1Rm / count); // 평균 1RM 설정
                recordDetails.setDetailsVolume(totalVolume); // 볼륨 설정
                recordDetails.setDetailsReps(totalReps); // 총 횟수 설정
            } else if (totalTimes > 0) { // 시간일때
                recordDetails.setSumTimes(totalTimes); // 총 시간 설정
            } else { // 횟수일때
                recordDetails.setMaxReps(maxReps); // 최대 횟수 설정
                recordDetails.setDetailsReps(totalReps); // 총 횟수 설정
            }

            newDetails.add(recordDetails);
        }

        recordDetailsRepository.saveAll(newDetails);
        recordBatchService.insertSetsInBatch(newSets);      // 배치를 통한 대량 저장

        int totalCalorie = calculateTotalCalorie(record.getStartTime(), record.getEndTime(), member, exerciseSize, totalMets);  // 수정된 데이터의 칼로리 계산
        record.updateRecord(recordVolume, totalCalorie,recordSets); // 최종 기록 도메인 데이터 수정

        recordBatchService.deleteDetailsIdsInBatch(deleteDetailsIds);   // 비동기로 삭제
    }

    /**
     * 기록 삭제 - 작성자만이 기록을 삭제 가능하다.
     * @param memberId 로그인한 사용자
     * @param recordId 삭제할 기록 ID
     */
    @Override
    public void deleteRecord(Long memberId, Long recordId) {
        Record record = recordRepository.findByIdAndMemberId(recordId, memberId);
        if (record==null) throw new InvalidUsageException(FORBIDDEN,AUTHOR_ONLY);
        recordRepository.deleteRecord(recordId,memberId);
    }

    /**
     * 해당 사용자의 운동 기록을 달력, 모든 데이터를 조회하여 RecordMonthResponse 적재
     * @param memberId  로그인한 사용자 ID
     * @param lastTime  마지막 시간
     * @param pageable  페이징 저보
     * @return  RecordMonthResponse반환
     */
    @Transactional(readOnly = true)
    @Override
    public RecordMonthResponse getRecordForMember(Long memberId, LocalDateTime lastTime, Pageable pageable) {
        Map<String, RecordData> recordCountAdnDataForMonth = recordRepository.getRecordCountAdnDataForMonth(memberId);  //일별 기록
        Slice<RecordDay> recordDays = recordRepository.getRecordPage(memberId, lastTime, pageable); // 해당달 전제 데이터
        RecordPage recordPage = new RecordPage(recordDays.hasNext(),recordDays.getContent());
        return new RecordMonthResponse(recordCountAdnDataForMonth,recordPage);

    }

    /*
    상세 운동의 대한 횟수를 새롭게 저장하는 메서드
     */
    private RecordSets recordSets(RecordDetails recordDetail, RecordSetsDto newSet) {
        Integer sets = newSet.getReps();        // 세트수
        Integer kg = newSet.getWeight();            // kg
        Integer times = newSet.getTimes();      // 시간수
        RecordSets recordSets;
        if (sets != null & kg != null) {        // 무게-세트 기준
            recordSets = RecordSets.ofWeightAndSets(kg, sets, recordDetail);
        } else if (times != null) {             // 시간 기준
            recordSets = RecordSets.ofTimes(times, sets,recordDetail);
        } else                                  // 세트 기준
            recordSets = RecordSets.ofSets(sets, recordDetail);
        return recordSetsRepository.save(recordSets);
    }

    /*
    최대 1RM 무게를 계산하는 메서드
     */

    private int calculate1RM(Integer weight, Integer reps) {
        return Math.round(weight * (1 + 0.03333f * reps));
    }
    private CustomExercise getCustomExercise(Long exerciseId) {
        return customExerciseRepository.findById(exerciseId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }

    private Exercise getExercise(Long exerciseId) {
        return exerciseRepository.findById(exerciseId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }

    /*
    총 소모 칼로리 계산 메서드
     */
    private static int calculateTotalCalorie(LocalDateTime startTime, LocalDateTime endTime, Member member, int exerciseSize, float totalMets) {
        int totalCalorie = (int) ((int) (totalMets / exerciseSize) * member.getWeight() * (int)Duration.between(startTime, endTime).toHours());
        return totalCalorie;
    }
}
