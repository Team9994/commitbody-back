package team9499.commitbody.domain.exercise.service.Impl;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.UpdateByQueryRequest;
import co.elastic.clients.elasticsearch.core.UpdateByQueryResponse;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.ExerciseInterest;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseInterestRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.exercise.service.ExerciseInterestService;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.Exception.ServerException;

import static team9499.commitbody.global.Exception.ExceptionStatus.*;
import static team9499.commitbody.global.Exception.ExceptionStatus.INTERNAL_SERVER_ERROR;
import static team9499.commitbody.global.Exception.ExceptionType.*;

@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class ExerciseInterestServiceImpl implements ExerciseInterestService {

    private final CustomExerciseRepository customExerciseRepository;
    private final ExerciseInterestRepository exerciseInterestRepository;
    private final ExerciseRepository exerciseRepository;
    private final MemberRepository memberRepository;

    private final String DEFAULT ="default_";
    private final String CUSTOM ="custom_";
    private final String ADD = "등록";
    private final String CANCEL ="해제";

    /**
     * 관심 운동 등록
     * DB에 데이터가 없을시 데이터를 저장한후 상태코드이 따라 관심 운동 상태를를 확인합니다.
     * 상태가 false 경우 일정시간 동안 유지 될경우 데이터의 최적화를 위해서 삭제합니다.(스케쥴러를 통한 최적화)
     * @return ADD = 등록 , CANCEL = 해제
     */
    @Override
    public String updateInterestStatus(Long exerciseId, Long memberId, String source) {

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchException(BAD_REQUEST, No_SUCH_MEMBER));

        ExerciseInterest exerciseInterest = findOrCreateInterest(exerciseId, memberId, source, member);
        if (exerciseInterest.isInterested()) {
            exerciseInterest.changeInterested(false);       // 관심 해제
            return CANCEL;
        } else {
            exerciseInterest.changeInterested(true);        // 관심 등록
            return ADD;
        }
    }

    /**
     * 상세운동 조회시 관심 운동 체크 메서드
     * @param exerciseId    운동 ID
     * @param memberId      로그인한 사용자 ID
     * @return 관심운동시 : true , 비관심 운동시 : false
     */
    @Override
    public boolean checkInterestStatus(Long exerciseId, Long memberId) {

        ExerciseInterest defaultExercise = exerciseInterestRepository.findByExerciseIdAndMemberId(exerciseId, memberId).orElse(null);
        if (defaultExercise != null && defaultExercise.isInterested()) {
            return true;
        }
        ExerciseInterest customExercise = exerciseInterestRepository.findByCustomExerciseIdAndMemberId(exerciseId, memberId).orElse(null);
        if (customExercise != null &&  customExercise.isInterested()) {
            return true;
        }

        return false;
    }


    /*
    관심 운동이 저장되어 있지 않을 경우 데이터를 저장하고 저장한 데이터를 반환하는 메서드
     */
    private ExerciseInterest findOrCreateInterest(Long exerciseId, Long memberId, String source, Member member) {
        if (source.equals(DEFAULT)|| source.equals("default")) {
            return exerciseInterestRepository.findByExerciseIdAndMemberId(exerciseId, memberId)
                    .orElseGet(() -> createNewInterest(exerciseId, source, member));
        }else {
            return exerciseInterestRepository.findByCustomExerciseIdAndMemberId(exerciseId, memberId)
                    .orElseGet(() -> createNewInterest(exerciseId, source, member));
        }
    }

    /*
    중복된 관심운동 Entity 조회를 운동 생성 타입의 따라 반환하는 메서드
     */
    private ExerciseInterest createNewInterest(Long exerciseId, String source, Member member) {
        if (source.equalsIgnoreCase(DEFAULT)) {         // 기본으로 제공되는 운동일 경우
            Exercise exercise = exerciseRepository.findById(exerciseId)
                    .orElseThrow(() -> new NoSuchException(BAD_REQUEST, NO_SUCH_DATA));
            return exerciseInterestRepository.save(ExerciseInterest.exerciseInterest(member, exercise));
        } else if (source.equalsIgnoreCase(CUSTOM)) {   // 사용자가 등록한 커스텀 운동일 경우
            CustomExercise customExercise = customExerciseRepository.findById(exerciseId)
                    .orElseThrow(() -> new NoSuchException(BAD_REQUEST, NO_SUCH_DATA));
            return exerciseInterestRepository.save(ExerciseInterest.exerciseInterest(member, customExercise));
        } else {
            throw new ServerException(INTERNAL_SERVER_ERROR, SERVER_ERROR);
        }
    }
}
