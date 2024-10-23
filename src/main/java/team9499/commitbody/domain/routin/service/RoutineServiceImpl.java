package team9499.commitbody.domain.routin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.dto.CustomExerciseDto;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.routin.domain.Routine;
import team9499.commitbody.domain.routin.domain.RoutineDetails;
import team9499.commitbody.domain.routin.dto.RoutineDto;
import team9499.commitbody.domain.routin.dto.response.MyRoutineResponse;
import team9499.commitbody.domain.routin.dto.rqeust.RoutineExercise;
import team9499.commitbody.domain.routin.repository.RoutineDetailsRepository;
import team9499.commitbody.domain.routin.repository.RoutineRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.Exception.ServerException;

import java.util.*;
import team9499.commitbody.global.redis.RedisService;

@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class RoutineServiceImpl implements RoutineService{

    private final RoutineRepository routineRepository;
    private final RoutineDetailsRepository routineDetailsRepository;
    private final ExerciseRepository exerciseRepository;
    private final CustomExerciseRepository customExerciseRepository;
    private final RoutineBatchService routineBatchService;
    private final RedisService redisService;

    private final String DEFAULT ="default";

    /**
     * 루틴을 저장
     * @param memberId 사용자 ID
     * @param routineExercises  루틴 정보
     * @param routineName   루틴 명
     */
    @Override
    public void saveRoutine(Long memberId, List<RoutineExercise> routineExercises, String routineName) {

        Member member = redisService.getMemberDto(memberId.toString()).get();

        Routine routine = routineRepository.save(Routine.create(member, routineName));  // 새로운 루틴 생성 및 저장
        List<RoutineDetails> routineDetails = new ArrayList<>();

        for (RoutineExercise routineExercise : routineExercises) {
            addRoutineDetails(routineExercise.getExerciseId(),routineExercise.getOrder(), routine, routineDetails,
                    !routineExercise.getSource().equals(DEFAULT));
        }
        // 루틴의 상세 목록 설정 및 저장
        routine.setList(routineDetails);
        routineDetailsRepository.saveAll(routineDetails);
    }

    /**
     * 사용자가 등록한 루틴의 대해서 루틴을 모두 조회합니다.
     * @param memberId 로그인한 사용자 ID
     * @return  루틴 객체
     */
    @Override
    public MyRoutineResponse getMyRoutine(Long memberId) {
        List<Routine> allByMemberId = routineRepository.findAllByMemberId(memberId);
        List<RoutineDto> routineDtos = new ArrayList<>();

        for (Routine routine : allByMemberId) {            // 각 루틴을 DTO로 변환하여 리스트에 추가
            RoutineDto routineDto = convertToRoutineDto(routine);
            routineDtos.add(routineDto);
        }
        // 오름차순 정렬
        sortAsc(routineDtos);
        return new MyRoutineResponse(routineDtos);    // 변환된 루틴 DTO 리스트를 MyRoutineResponse로 반환
    }

    /**
     * 특정 루틴의 루틴 상세 조회
     * @param memberId  사용자 ID
     * @param routineId 조회할 루틴 ID
     * @return  상세 조회한 루틴 객체
     */
    @Override
    public MyRoutineResponse getDetailMyRoutine(Long memberId, Long routineId) {
        Optional<Routine> byIdAndMemberId = routineRepository.findByIdAndMemberId(routineId, memberId);
        List<RoutineDto> routineDtos = new ArrayList<>();
        if (byIdAndMemberId.isPresent()){
            Routine routine = byIdAndMemberId.get();
            RoutineDto routineDto = convertToRoutineDto(routine);
            routineDtos.add(routineDto);
        }

        // 오름차순 정렬
        sortAsc(routineDtos);

        return new MyRoutineResponse(routineDtos);    // 변환된 루틴 DTO 리스트를 MyRoutineResponse로 반환
    }

    /**
     * 루틴 편집에 사용되는 메서드
     * 값이 NULL 아닐시에만  사용되도록 작성
     * @param routineId 최상위 루틴 Id
     * @param memberId 로그인한 사용자 Id
     * @param routineName 변경할 루틴 명
     * @param exercises 변경할 루틴의 상세 루틴 운동종류
     */
    @Override
    public void updateRoutine(Long routineId, Long memberId, String routineName, List<ExerciseDto> exercises) {
        Routine routine = routineRepository.findByIdAndMemberId(routineId,memberId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
        // 루틴명 변경
        routine.updateRoutineName(routineName);

        // 루틴의 저장된 상세루틴 ID 리스트
        List<Long> deleteDetailIds = new ArrayList<>();
        //순회하며 id 적재
        routine.getList().forEach(routineDetails -> deleteDetailIds.add(routineDetails.getId()));

        //새롭게 저장할 새로운 루틴 운동
        List<RoutineDetails> newRoutineDetails = new ArrayList<>();

        for (ExerciseDto exercise : exercises) {
            String source = exercise.getSource();
            Object obExercise = source.equals(DEFAULT) ? getExercise(exercise.getExerciseId()) : getCustomExercise(exercise.getExerciseId());       // 운동과 커스텀 운동 구분

            newRoutineDetails.add(RoutineDetails.of(exercise.getRoutineDetailId(), obExercise, routine, exercise.getOrders()));
        }

        // 배치를 통해 삭제
        routineDetailsRepository.deleteAllByInQuery(deleteDetailIds);
        // 배치를 통해 저장
        routineBatchService.saveRoutineDetailInBatch(newRoutineDetails);
    }

    /**
     * 루틴 삭제
     * 루틴은 작성자만 삭제 가능합니다.
     * @param routineId 루틴 아이디
     * @param memberId  현재 로그인한 사용자 ID
     */
    @Override
    public void deleteRoutine(Long routineId,Long memberId) {
        routineRepository.findByIdAndMemberId(routineId,memberId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST,ExceptionType.NO_SUCH_DATA));
        routineRepository.deleteRoutine(routineId);
    }

    private CustomExercise getCustomExercise(Long exerciseId) {
        return customExerciseRepository.findById(exerciseId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }

    private Exercise getExercise(Long exerciseId) {
        return exerciseRepository.findById(exerciseId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
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
     * 주어진 exerciseIds 목록을 순회하면서 루틴 상세 정보를 추가하는 메서드
     */
    private void addRoutineDetails(Long exerciseId,Integer order, Routine routine, List<RoutineDetails> routineDetails,boolean isCustom) {
        if (isCustom) {         // 커스텀 운동인 경우 처리
            CustomExercise customExercise = customExerciseRepository.findById(exerciseId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
            routineDetails.add(RoutineDetails.of(customExercise, routine,order));
        } else {                // 일반 운동인 경우 처리
            Exercise exercise = exerciseRepository.findById(exerciseId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
            routineDetails.add(RoutineDetails.of(exercise, routine,order));
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

        if (exercise != null) {             // 기본 운동 정보가 있으면 대상에 추가하고, 운동을 리스트에 추가
            targets.add(exercise.getExerciseTarget().name());
            exercises.add(ExerciseDto.of(routineDetailsId,exercise.getId(),exercise.getExerciseName(), exercise.getGifUrl(), routineDetails.getTotalSets(),exercise.getExerciseType().getDescription(), routineDetails.getOrders()));
        } else {                // 커스텀 운동 정보가 있으면 대상에 추가하고, 커스텀 운동을 리스트에 추가
            CustomExercise customExercise = routineDetails.getCustomExercise();
            targets.add(customExercise.getExerciseTarget().name());
            exercises.add(CustomExerciseDto.of(routineDetailsId, customExercise.getId(),customExercise.getCustomExName(), customExercise.getCustomGifUrl(), routineDetails.getTotalSets(),"무게와 횟수", routineDetails.getOrders()));
        }
    }

    /*
    오름 차순으 정렬
     */
    private void sortAsc(List<RoutineDto> routineDtos) {
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
    }

}
