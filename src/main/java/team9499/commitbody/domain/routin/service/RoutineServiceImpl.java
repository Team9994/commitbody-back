package team9499.commitbody.domain.routin.service;

import lombok.RequiredArgsConstructor;
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

import static team9499.commitbody.global.constants.ElasticFiled.*;

@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class RoutineServiceImpl implements RoutineService {

    private final RoutineRepository routineRepository;
    private final RoutineDetailsRepository routineDetailsRepository;
    private final ExerciseRepository exerciseRepository;
    private final CustomExerciseRepository customExerciseRepository;
    private final RoutineBatchService routineBatchService;
    private final RedisService redisService;

    /**
     * 루틴을 저장
     */
    @Override
    public void saveRoutine(Long memberId, List<RoutineExercise> routineExercises, String routineName) {
        Routine routine = createAndSaveRoutine(memberId, routineName);
        saveRoutineDetails(routineExercises, routine);
    }

    /**
     * 사용자가 등록한 루틴의 대해서 루틴을 모두 조회합니다.
     */
    @Override
    public MyRoutineResponse getMyRoutine(Long memberId) {
        List<Routine> routines = findRoutinesByMemberId(memberId);
        List<RoutineDto> routineDtos = mapRoutinesToDtos(routines);
        sortAsc(routineDtos);
        return new MyRoutineResponse(routineDtos);
    }

    /**
     * 특정 루틴의 루틴 상세 조회
     */
    @Override
    public MyRoutineResponse getDetailMyRoutine(Long memberId, Long routineId) {
        Optional<Routine> byIdAndMemberId = routineRepository.findByIdAndMemberId(routineId, memberId);
        List<RoutineDto> routineDtos = getRoutineDtos(byIdAndMemberId);
        sortAsc(routineDtos);
        return new MyRoutineResponse(routineDtos);
    }

    /**
     * 루틴 편집에 사용되는 메서드
     * 값이 NULL 아닐시에만  사용되도록 작성
     */
    @Override
    public void updateRoutine(Long routineId, Long memberId, String routineName, List<ExerciseDto> exercises) {
        Routine routine = getRoutine(routineId, memberId);
        routine.updateRoutineName(routineName);
        routineDetailsRepository.deleteAllByInQuery(getDeleteDetailIds(routine));
        routineBatchService.saveRoutineDetailInBatch(getNewRoutineDetails(exercises, routine));
    }

    /**
     * 루틴 삭제
     * 루틴은 작성자만 삭제 가능합니다.
     *
     * @param routineId 루틴 아이디
     * @param memberId  현재 로그인한 사용자 ID
     */
    @Override
    public void deleteRoutine(Long routineId, Long memberId) {
        getRoutine(routineId, memberId);
        routineRepository.deleteRoutine(routineId);
    }


    // === saveRoutine() === //
    private Routine createAndSaveRoutine(Long memberId, String routineName) {
        return routineRepository.save(Routine.create(getMember(memberId), routineName));
    }

    private void saveRoutineDetails(List<RoutineExercise> routineExercises, Routine routine) {
        List<RoutineDetails> routineDetails = new ArrayList<>();
        populateRoutineDetails(routineExercises, routine, routineDetails);
        routine.setList(routineDetails);
        routineDetailsRepository.saveAll(routineDetails);
    }

    private Member getMember(Long memberId) {
        return redisService.getMemberDto(memberId.toString()).get();
    }

    private void populateRoutineDetails(List<RoutineExercise> routineExercises, Routine routine, List<RoutineDetails> routineDetails) {
        for (RoutineExercise routineExercise : routineExercises) {
            addRoutineDetails(routineExercise.getExerciseId(), routineExercise.getOrder(), routine, routineDetails,
                    !routineExercise.getSource().equals(DEFAULT));
        }
    }

    private void addRoutineDetails(Long exerciseId, Integer order, Routine routine, List<RoutineDetails> routineDetails, boolean isCustom) {
        if (isCustom) {         // 커스텀 운동인 경우 처리
            routineDetails.add(RoutineDetails.of(getCustomExercise(exerciseId), routine, order));
            return;
        }                // 일반 운동인 경우 처리
        routineDetails.add(RoutineDetails.of(getExercise(exerciseId), routine, order));
    }


    // === getMyRoutine() === //
    private List<Routine> findRoutinesByMemberId(Long memberId) {
        return routineRepository.findAllByMemberId(memberId);
    }

    private List<RoutineDto> mapRoutinesToDtos(List<Routine> routines) {
        List<RoutineDto> routineDtos = new ArrayList<>();
        for (Routine routine : routines) {
            RoutineDto routineDto = convertToRoutineDto(routine);
            routineDtos.add(routineDto);
        }
        return routineDtos;
    }

    private RoutineDto convertToRoutineDto(Routine routine) {
        RoutineDto routineDto = RoutineDto.of(routine);
        setTargetAndExercises(routine.getList(), routineDto);
        return routineDto;
    }

    private void setTargetAndExercises(List<RoutineDetails> list, RoutineDto routineDto) {
        Set<String> targets = new HashSet<>();
        List<Object> exercises = new ArrayList<>();
        for (RoutineDetails routineDetails : list) {
            addExerciseInfo(routineDetails, targets, exercises);
        }
        routineDto.setData(targets, exercises);
    }

    private void addExerciseInfo(RoutineDetails routineDetails, Set<String> targets, List<Object> exercises) {
        Exercise exercise = routineDetails.getExercise(); // 루틴 상세 내 운동 정보를 가져옴
        if (exercise != null) {             // 기본 운동 정보가 있으면 대상에 추가하고, 운동을 리스트에 추가
            targets.add(exercise.getExerciseTarget().name());
            exercises.add(ExerciseDto.of(routineDetails, exercise));
            return;
        }                // 커스텀 운동 정보가 있으면 대상에 추가하고, 커스텀 운동을 리스트에 추가
        CustomExercise customExercise = routineDetails.getCustomExercise();
        targets.add(customExercise.getExerciseTarget().name());
        exercises.add(CustomExerciseDto.of(routineDetails, customExercise));
    }

    // === getDetailMyRoutine() === //

    private List<RoutineDto> getRoutineDtos(Optional<Routine> byIdAndMemberId) {
        List<RoutineDto> routineDtos = new ArrayList<>();
        if (byIdAndMemberId.isPresent()) {
            Routine routine = byIdAndMemberId.get();
            RoutineDto routineDto = convertToRoutineDto(routine);
            routineDtos.add(routineDto);
        }
        return routineDtos;
    }

    // === updateRoutine() === //
    private static List<Long> getDeleteDetailIds(Routine routine) {
        List<Long> deleteDetailIds = new ArrayList<>();
        routine.getList().forEach(routineDetails -> deleteDetailIds.add(routineDetails.getId()));
        return deleteDetailIds;
    }

    private List<RoutineDetails> getNewRoutineDetails(List<ExerciseDto> exercises, Routine routine) {
        List<RoutineDetails> newRoutineDetails = new ArrayList<>();
        for (ExerciseDto exerciseDto : exercises) {
            addNewRoutineDetails(routine, exerciseDto, newRoutineDetails);
        }
        return newRoutineDetails;
    }

    private void addNewRoutineDetails(Routine routine, ExerciseDto exerciseDto, List<RoutineDetails> newRoutineDetails) {
        Object obExercise = exerciseDto.getSource().equals(DEFAULT) ?
                getExercise(exerciseDto.getExerciseId()) : getCustomExercise(exerciseDto.getExerciseId());       // 운동과 커스텀 운동 구분
        newRoutineDetails.add(RoutineDetails.of(exerciseDto, obExercise, routine));
    }

    // === 공용 메서드 () === //
    private CustomExercise getCustomExercise(Long exerciseId) {
        return customExerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }

    private Exercise getExercise(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }

    private Routine getRoutine(Long routineId, Long memberId) {
        return routineRepository.findByIdAndMemberId(routineId, memberId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }

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

    private Integer getOrder(Object obj) {
        if (obj instanceof ExerciseDto) {
            return ((ExerciseDto) obj).getOrders();
        } else if (obj instanceof CustomExerciseDto) {
            return ((CustomExerciseDto) obj).getOrders();
        }
        throw new ServerException(ExceptionStatus.INTERNAL_SERVER_ERROR, ExceptionType.SERVER_ERROR);
    }
}
