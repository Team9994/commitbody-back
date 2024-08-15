package team9499.commitbody.domain.record.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.Exercise;
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
import team9499.commitbody.global.Exception.NoSuchException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // TODO: 2024-08-14 리팩토링 필요
    /**
     * 루틴 완료시 루틴의 대한기록을 저장하는 메서드
     * @param memberId 사용자 ID
     * @param recordName 기록명
     * @param startTime 시작 시간
     * @param endTime 마무리 시간
     */
    @Override
    public void saveRecord(Long memberId, String recordName, LocalDateTime startTime, LocalDateTime endTime, List<RecordDto> recordDtos) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, No_SUCH_MEMBER));
        int totalVolume = 0;        // 투틴 총 수행 볼륨
        int totalSets = 0;          // 루틴 총 진행 세트
        int exerciseSize = 0;       // 루틴의 총 운동수
        float totalMets = 0;        // 총 Mets값

        int exerciseDuration = (int) Duration.between(startTime, endTime).toMinutes();

        Record record = Record.create(recordName, startTime, endTime, exerciseDuration, member);

        List<RecordSets> recordSets = new ArrayList<>();
        List<RecordDetails> recordDetails = new ArrayList<>();

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

            RecordDetails recordDetail = RecordDetails.create(exercise,record);       //운동이 적용된 상세 루턴 객체 생성

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
                    detailsVolume += weight;
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
        int totalCalorie = (int) (totalMets / exerciseSize) * Integer.parseInt(member.getWeight()) * (int)Duration.between(startTime, endTime).toHours();

        record.setRecordSets(totalSets);
        record.setRecordVolume(totalVolume);;
        record.setRecordCalorie(totalCalorie);

        recordRepository.save(record);
        recordDetailsRepository.saveAll(recordDetails);
        recordSetsRepository.saveAll(recordSets);
    }

    @Transactional(readOnly = true)
    @Override
    public RecordResponse getRecord(Long memberId, Long recordId) {
        return recordRepository.findByRecordId(recordId,memberId);
    }
}
