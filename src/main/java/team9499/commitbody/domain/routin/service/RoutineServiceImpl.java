package team9499.commitbody.domain.routin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.routin.domain.Routine;
import team9499.commitbody.domain.routin.domain.RoutineDetails;
import team9499.commitbody.domain.routin.repository.RoutineDetailsRepository;
import team9499.commitbody.domain.routin.repository.RoutineRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;

import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RoutineServiceImpl implements RoutineService{

    private final RoutineRepository routineRepository;
    private final RoutineDetailsRepository routineDetailsRepository;
    private final MemberRepository memberRepository;
    private final ExerciseRepository exerciseRepository;
    private final CustomExerciseRepository customExerciseRepository;

    @Override
    public void saveRoutine(Long memberId, List<Long> exerciseIds, List<Long> customExerciseIds, String routineName) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));

        Routine routine = routineRepository.save(Routine.create(member, routineName));  // 새로운 루틴 생성 및 저장
        List<RoutineDetails> routineDetails = new ArrayList<>();

        if (exerciseIds != null) {      // exerciseIds가 null이 아닌 경우 처리
            addRoutineDetails(exerciseIds, routine, routineDetails, false);
        }
        if (customExerciseIds != null) {        // customExerciseIds가 null이 아닌 경우 처리
            addRoutineDetails(customExerciseIds, routine, routineDetails, true);
        }

        // 루틴의 상세 목록 설정 및 저장
        routine.setList(routineDetails);
        routineDetailsRepository.saveAll(routineDetails);
    }

    /**
     * 주어진 exerciseIds 목록을 순회하면서 루틴 상세 정보를 추가하는 메서드
     */
    private void addRoutineDetails(List<Long> exerciseIds, Routine routine, List<RoutineDetails> routineDetails, boolean isCustom) {
        for (Long id : exerciseIds) {
            if (isCustom) {         // 커스텀 운동인 경우 처리
                CustomExercise customExercise = customExerciseRepository.findById(id).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
                routineDetails.add(RoutineDetails.of(customExercise, routine));
            } else {                // 일반 운동인 경우 처리
                Exercise exercise = exerciseRepository.findById(id).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
                routineDetails.add(RoutineDetails.of(exercise, routine));
            }
        }
    }

}
