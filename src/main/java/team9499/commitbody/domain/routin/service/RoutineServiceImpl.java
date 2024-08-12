package team9499.commitbody.domain.routin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.dto.CustomExerciseDto;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.routin.domain.RoutineSets;
import team9499.commitbody.domain.routin.domain.Routine;
import team9499.commitbody.domain.routin.domain.RoutineDetails;
import team9499.commitbody.domain.routin.dto.RoutineDto;
import team9499.commitbody.domain.routin.dto.RoutineSetsDto;
import team9499.commitbody.domain.routin.dto.response.MyRoutineResponse;
import team9499.commitbody.domain.routin.repository.RoutineDetailsRepository;
import team9499.commitbody.domain.routin.repository.RoutineRepository;
import team9499.commitbody.domain.routin.repository.RoutineSetsRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.Exception.ServerException;

import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RoutineServiceImpl implements RoutineService{

    private final RoutineRepository routineRepository;
    private final RoutineDetailsRepository routineDetailsRepository;
    private final RoutineSetsRepository routineSetsRepository;
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
        List<RoutineSets> routineSets = new ArrayList<>();

        // 루틴 등록시 초기 세트수 저장
        for (RoutineDetails routineDetail : routineDetails) {
            Integer totalSets = routineDetail.getTotalSets();
            switch (totalSets){
                case 5 ->{
                    for (int i =0; i<5;i++){
                        routineSets.add(RoutineSets.ofWeightAndSets(20,10,routineDetail));
                    }
                }
                case 4->{
                    for (int i = 0; i<4;i++){
                        routineSets.add(RoutineSets.ofSets(10,routineDetail));
                    }
                }
                default -> {
                    routineSets.add(RoutineSets.ofTimes(60,routineDetail));
                }
            }
        }
        routineSetsRepository.saveAll(routineSets);
    }


    /**
     * 주어진 exerciseIds 목록을 순회하면서 루틴 상세 정보를 추가하는 메서드
     */
    private void addRoutineDetails(List<Long> exerciseIds, Routine routine, List<RoutineDetails> routineDetails, boolean isCustom) {
        for(int i =0; i<exerciseIds.size();i++){
            if (isCustom) {         // 커스텀 운동인 경우 처리
                CustomExercise customExercise = customExerciseRepository.findById(exerciseIds.get(i)).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
                routineDetails.add(RoutineDetails.of(customExercise, routine,i+1));
            } else {                // 일반 운동인 경우 처리
                Exercise exercise = exerciseRepository.findById(exerciseIds.get(i)).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
                routineDetails.add(RoutineDetails.of(exercise, routine,i+1));
            }
        }
    }



    @Override
    public MyRoutineResponse getMyRoutine(Long memberId) {
        List<Routine> allByMemberId = routineRepository.findAllByMemberId(memberId);
        List<RoutineDto> routineDtos = new ArrayList<>();

        for (Routine routine : allByMemberId) {            // 각 루틴을 DTO로 변환하여 리스트에 추가
            RoutineDto routineDto = convertToRoutineDto(routine);
            routineDtos.add(routineDto);
        }

        // 오름차순 정렬
        for (RoutineDto routineDto : routineDtos) {
            List<Object> exercises = routineDto.getExercises();
            if (exercises != null) {
                exercises.sort((o1, o2) -> {
                    Integer orders1 = getOrder(o1);
                    Integer orders2 = getOrder(o2);
                    return orders1.compareTo(orders2);
                });
            }
        }
        return new MyRoutineResponse(routineDtos);    // 변환된 루틴 DTO 리스트를 MyRoutineResponse로 반환
    }

    // orders 기준으로 정렬
    private Integer getOrder(Object obj) {
        if (obj instanceof ExerciseDto) {
            return ((ExerciseDto) obj).getOrders();
        } else if (obj instanceof CustomExerciseDto) {
            return ((CustomExerciseDto) obj).getOrders();
        } else {
            log.error("정렬증 오류 발생");
            throw new ServerException(ExceptionStatus.INTERNAL_SERVER_ERROR,ExceptionType.SERVER_ERROR);
        }
    }

    /**
     * 주어진 루틴을 RoutineDto로 변환하는 메서드
     */
    private RoutineDto convertToRoutineDto(Routine routine) {
        RoutineDto routineDto = new RoutineDto();
        routineDto.setRoutineName(routine.getRoutineName());
        routineDto.setRoutineId(routine.getId());

        List<RoutineDetails> list = routine.getList();

        // 운동 대상과 운동 리스트를 저장할 Set 및 List 초기화
        Set<String> targets = new HashSet<>();
        List<Object> exercises = new ArrayList<>();

        // 루틴 상세 목록을 순회하며 운동 정보를 추가
        for (RoutineDetails routineDetails : list) {
            addExerciseInfo(routineDetails, targets, exercises);
        }

        routineDto.setTargets(targets);
        routineDto.setExercises(exercises);

        return routineDto;
    }

    private void addExerciseInfo(RoutineDetails routineDetails, Set<String> targets, List<Object> exercises) {
        Exercise exercise = routineDetails.getExercise(); // 루틴 상세 내 운동 정보를 가져옴
        Long routineDetailsId = routineDetails.getId();
        
        List<RoutineSets> allByRoutineDetailsId = routineSetsRepository.findAllByRoutineDetailsId(routineDetailsId);
        List<RoutineSetsDto> routineSetsDtos = new ArrayList<>();
        
        // 루틴의 저장된 각 운동의 세트스를 리스트에 추가
        for (RoutineSets routineSets : allByRoutineDetailsId) {
            routineSetsDtos.add(RoutineSetsDto.fromDto(routineSets));
        }
        if (exercise != null) {             // 기본 운동 정보가 있으면 대상에 추가하고, 운동을 리스트에 추가

            targets.add(exercise.getExerciseTarget().name());
            exercises.add(ExerciseDto.of(routineDetailsId,exercise.getId(), exercise.getExerciseName(), exercise.getGifUrl(), routineDetails.getTotalSets(),exercise.getExerciseType().getDescription(), routineDetails.getOrders(), routineSetsDtos));
        } else {                // 커스텀 운동 정보가 있으면 대상에 추가하고, 커스텀 운동을 리스트에 추가
            CustomExercise customExercise = routineDetails.getCustomExercise();
            targets.add(customExercise.getExerciseTarget().name());
            exercises.add(CustomExerciseDto.of(routineDetailsId, customExercise.getId(), customExercise.getCustomExName(), customExercise.getCustomGifUrl(), routineDetails.getTotalSets(),"무게와 횟수", routineDetails.getOrders(), routineSetsDtos));
        }
    }

}
