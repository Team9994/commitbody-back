package team9499.commitbody.domain.record.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.record.domain.Record;
import team9499.commitbody.domain.record.domain.RecordDetails;
import team9499.commitbody.domain.record.domain.RecordSets;
import team9499.commitbody.domain.record.dto.RecordDto;
import team9499.commitbody.domain.record.dto.RecordSetsDto;
import team9499.commitbody.domain.record.dto.response.RecordResponse;
import team9499.commitbody.domain.record.repository.RecordDetailsRepository;
import team9499.commitbody.domain.record.repository.RecordRepository;
import team9499.commitbody.domain.record.repository.RecordSetsRepository;
import team9499.commitbody.domain.routin.dto.RoutineSetsDto;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static team9499.commitbody.domain.record.dto.request.UpdateRecordRequest.*;
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

            for (RecordSetsDto set : recordDto.getSets()) {         // 운동별 기록을 저장하기 위해 순회
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


    // TODO: 2024-08-24 리팩토링 필요
    /**
     * 루틴 완료시 기록된 데이터를 수정하기 위한 메서드
     * @param memberId 로그인한 사용자 ID
     * @param recordId 변경할 recordID
     * @param updateSets  변경및 추가할 세트 클래스
     * @param newExercises 새롭게 추가할 운동 클래스
     * @param deleteSets 삭제할 세트 클래스
     * @param deleteExercises 삭제할 운동 클래스
     * @param changeOrders 상세 기록 순서를 변경할 클래스
     */
    @Override
    public void updateRecord(Long memberId, Long recordId, List<RecordUpdateSets> updateSets, List<ExerciseDto> newExercises, List<Long> deleteSets, List<Long> deleteExercises, List<ChangeOrders> changeOrders) {
        Record record = recordRepository.findByIdAndMemberId(recordId, memberId);
        // 수정 할 세트수가 존재시
        if (updateSets!=null){
            for (RecordUpdateSets updateSet : updateSets) {
                Long recordDetailsId = updateSet.getRecordDetailsId(); // 기록 디테일 ID
                if (updateSet.getUpdateSets()!=null){       // 수정할 세트수가 있을때
                    for (RoutineSetsDto set : updateSet.getUpdateSets()) {
                        RecordSets recordSets = recordSetsRepository.findByIdAndRecordDetailsId(set.getSetsId(), recordDetailsId);
                        Integer reps = set.getSets();
                        Integer weight = set.getKg();
                        Integer times = set.getTimes();

                        if (reps != null && weight != null) {   // 무게 횟수일때
                            recordSets.updateWeightAndReps(weight, reps);
                        } else if (times != null) {     // 시간일떄
                            recordSets.updateTimes(times);
                        } else {        // 횟수 일때
                            recordSets.updateReps(reps);
                        }
                    }
                }

                // 새롭게 등록할 세트 수 존재시
                if (updateSet.getNewSets()!=null){
                    RecordDetails details = recordDetailsRepository.findById(recordDetailsId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, NO_SUCH_DATA));
                    for (RoutineSetsDto set : updateSet.getNewSets()) {
                        recordSets(details,set);        // 새로운 세트 등록
                    }
                }
            }
        }

        // 추가 할 운동이 존재시
        if (newExercises!=null){
            List<RecordDetails> recordDetailsList = new ArrayList<>();
            for (ExerciseDto newExercise : newExercises) {
                RecordDetails newRecordDetails;
                Integer orders = newExercise.getOrders();

                if (newExercise.getSource().equals(DEFAULT)) {      // 일반 운동일 떄
                    Exercise exercise = getExercise(newExercise.getExerciseId());       // 운동 객체 조회
                    newRecordDetails = RecordDetails.of(exercise, record,orders);
                } else {        // 커스텀 운동일 때
                    CustomExercise customExercise = getCustomExercise(newExercise.getExerciseId());
                    newRecordDetails = RecordDetails.of(customExercise, record, orders);
                }
                // 기록 상세 저장
                RecordDetails recordDetails = recordDetailsRepository.save(newRecordDetails);
                // 세트 리스트를 생성
                List<RecordSets> recordSets = new ArrayList<>();
                for (RoutineSetsDto set : newExercise.getRoutineSets()) {
                    recordSets.add(recordSets(recordDetails, set)); // 세트 리스트에 담는다.
                }
                recordDetailsList.add(recordDetails);       // 기록 리스트에 담는다.
                newRecordDetails.setSetsList(recordSets);   // 양방향 세트 매핑에 사용할 리스트를 담는다.
            }
            record.setDetailsList(recordDetailsList);      // 양방향 레코드 매핑에 사용할 리스트를 담는다.
        }


        // 삭제 할 세트수가 존재시
        if (deleteSets !=null){
            for (Long setsId : deleteSets) {
                recordSetsRepository.deleteById(setsId);        // 세트 데이터 삭제
            }
            em.flush();     // 삭제후 데이터가 영속화 되어 있는 상태이기때문에 삭제된 데이터를 flush 를 통해 적용해준다.
        }

        // 삭제 할 운동이 존재시
        if (deleteExercises!=null){
            for (Long detailsId : deleteExercises) {
                recordDetailsRepository.deleteById(detailsId); // 상세 기록 삭제
            }
            em.flush();     // 삭제후 데이터가 영속화 되어 있는 상태이기때문에 삭제된 데이터를 flush 를 통해 적용해준다.
        }


        // 변경 할 운동 순서가 존재시
        if (changeOrders != null){
            for (ChangeOrders changeOrder : changeOrders) {
                // 변경할 상세 기록 데이터가 있는지 확인후 없으면 400 예외 발생
                RecordDetails details = recordDetailsRepository.findById(changeOrder.getRecordDetailsId()).orElseThrow(() -> new NoSuchException(BAD_REQUEST, NO_SUCH_DATA));
                details.updateOrder(changeOrder.getOrders());   // 순서 변경
            }
        }

        // 해당 기록에 저장된 상세기록 리스트를 가져온다.
        List<RecordDetails> detailsList = record.getDetailsList();
        int recordSets = 0;
        int exerciseSize = 0;
        int recordVolume = 0;
        float totalMets = 0;

        // 상세기록 리스트를 순환한다.
        for (RecordDetails details : detailsList) {
            recordSets++;
            exerciseSize++;
            int max1Rm = 0;     // 최대 1RM
            int count = 0;      // 무게+횟수 기록 운동 갯수
            int totalTimes = 0;     // 총 운동 시간(시간일때)
            int maxReps = 0;        // 총 운동 횟수(무게+횟수 , 횟수)
            int totalVolume = 0;    // 총 볼륨(무게 + 횟수)
            int totalSets = 0;      // 총 진행한 세트수
            int totalReps = 0;      // 총 진행한 횟수

            if (details.getExercise()!=null){
                totalMets += details.getExercise().getMets();
            }else
                totalMets += 4;
            // 세트 리스트를 순환한다.
            for (RecordSets recordSet :  details.getSetsList()) {
                Integer weight = recordSet.getWeight();     // 무게
                Integer reps = recordSet.getReps();         // 횟수
                Integer times = recordSet.getTimes();       // 시간
                totalSets++;        // 세트수 증가
                if (weight!=null&& reps!=null){     // 무게 + 횟수일 떄
                    max1Rm+= calculate1RM(weight,reps); //1RM 계산
                    totalVolume += reps*weight; // 총 볼륨 계산
                    recordVolume +=  reps*weight; // 기록 볼륨 누적
                    totalReps += reps;      // 횟수 계싼
                    count++;        // 운동 데이터 수 증가
                }else if (times!=null){ // 시간일때
                    totalTimes += times;
                }else{
                    maxReps = Math.max(reps,maxReps); // 최대 횟수 갱신
                    totalReps += reps;
                }
            }

            details.updateSets(totalSets);      // 총 세트수 업데이트
            if (details.getMax1RM()!=null){     // 무게 + 횟수 일때
                details.updateMax1RM(max1Rm/count); // 최대 1RM 갱신
                details.updateDetailsVolume(totalVolume);   // 총 볼륨 갱신
                details.updateDetailsReps(totalReps);       // 총 횟수 갱신
            }else if (details.getSumTimes()!=null){     // 시간일 떄
                details.updateDetailsTimes(totalTimes);     // 총 누적 시간 갱신
            }else{
                details.updateMaxReps(maxReps);     // 최대 횟수 갱신
                details.updateDetailsReps(totalReps);       // 운동 상세 갱신
            }
        }

        int totalCalorie = calculateTotalCalorie(record.getStartTime(), record.getEndTime(), record.getMember(), exerciseSize, totalMets);  // 수정된 데이터의 칼로리 계산
        record.updateRecord(recordVolume,totalCalorie,recordSets); // 최종 기록 도메인 데이터 수정
    }
    /*
    상세 운동의 대한 횟수를 새롭게 저장하는 메서드
     */

    private RecordSets recordSets(RecordDetails recordDetail, RoutineSetsDto newSet) {
        Integer sets = newSet.getSets();        // 세트수
        Integer kg = newSet.getKg();            // kg
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
