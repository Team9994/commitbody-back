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
import team9499.commitbody.domain.routin.dto.rqeust.RoutineExercise;
import team9499.commitbody.domain.routin.repository.RoutineDetailsRepository;
import team9499.commitbody.domain.routin.repository.RoutineRepository;
import team9499.commitbody.domain.routin.repository.RoutineSetsRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.Exception.ServerException;

import java.util.*;

import static team9499.commitbody.domain.routin.dto.rqeust.UpdateRoutineRequest.*;

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

    private final String DEFAULT ="default";

    @Override
    public void saveRoutine(Long memberId, List<RoutineExercise> routineExercises, String routineName) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));

        Routine routine = routineRepository.save(Routine.create(member, routineName));  // 새로운 루틴 생성 및 저장
        List<RoutineDetails> routineDetails = new ArrayList<>();

        for (RoutineExercise routineExercise : routineExercises) {
            String source = routineExercise.getSource();
            // 일반 운동일 경우
            if (source.equals(DEFAULT)) {
                addRoutineDetails(routineExercise.getExerciseId(),routineExercise.getOrder(), routine, routineDetails, false);
            }else { // 커스컴 운동일 경우
                addRoutineDetails(routineExercise.getExerciseId(),routineExercise.getOrder(), routine, routineDetails, true);
            }
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
    private void addRoutineDetails(Long exerciseId,Integer order, Routine routine, List<RoutineDetails> routineDetails,boolean isCustom) {
        if (isCustom) {         // 커스텀 운동인 경우 처리
            CustomExercise customExercise = customExerciseRepository.findById(exerciseId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
            routineDetails.add(RoutineDetails.of(customExercise, routine,order));
        } else {                // 일반 운동인 경우 처리
            Exercise exercise = exerciseRepository.findById(exerciseId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
            routineDetails.add(RoutineDetails.of(exercise, routine,order));
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

    @Override
    public MyRoutineResponse getDetailMyRoutine(Long memberId, Long routineId) {
        Optional<Routine> byIdAndMemberId = routineRepository.findByIdAndMemberId(routineId, memberId);
        List<RoutineDto> routineDtos = new ArrayList<>();
        if (!byIdAndMemberId.isEmpty()){
            Routine routine = byIdAndMemberId.get();
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

    /**
     * 루틴 편집에 사용되는 메서드
     * 값이 NULL 아닐시에만  사용되도록 작성
     * @param routineId 최상위 루틴 Id
     * @param memberId 로그인한 사용자 Id
     * @param routineName 변경할 루틴 명
     * @param deleteRoutines 삭제할 상세루틴 아이디 목록
     * @param updateSets 업데이트할 세트수 목록
     * @param deleteSets 삭제할 세트수 목록
     * @param newExercises 새로 추가할 운동 목록
     * @param changeExercises 운동을 대채할 운동 목록
     * @param changeOrders 운동의 순서를 변경 목록
     */
    @Override
    public void updateRoutine(Long routineId, Long memberId, String routineName, List<Long> deleteRoutines, List<UpdateSets> updateSets, List<DeleteSets> deleteSets,
                              List<ExerciseDto> newExercises, List<ChangeExercise> changeExercises,List<ChangeOrders> changeOrders) {

        // 사용자가 작성한 루틴 조회
        Routine routine = routineRepository.findByIdAndMemberId(routineId, memberId)
                .orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));

        // 변경할 루틴명이 존재시
        if (routineName != null) {
            routine.updateRoutineName(routineName);
        }

        // 삭제할 상세 루틴이 존재시
        if (deleteRoutines != null) {
            routineDetailsRepository.deleteAllById(deleteRoutines);
        }

        // 변경할 세트스 존재시
        if (updateSets != null) {
            for (UpdateSets updateSet : updateSets) {
                Long routineDetailsId = updateSet.getRoutineDetailsId();
                // 세트수 변경시
                if (updateSet.getUpdateSets() != null) {
                    for (RoutineSetsDto routineSet : updateSet.getUpdateSets()) {
                        RoutineSets routineSets = routineSetsRepository.findByIdAndRoutineDetailsId(routineSet.getSetsId(), routineDetailsId);
                        Integer sets = routineSet.getSets();
                        Integer kg = routineSet.getKg();
                        Integer times = routineSet.getTimes();
                        if (sets != null & kg != null) {
                            routineSets.updateWeightAndSets(kg, sets);
                        } else if (times != null) {
                            routineSets.updateTimes(times);
                        } else
                            routineSets.updateSets(sets);
                    }
                }
                // 새로 등록할 세트스가 존재시
                if (updateSet.getNewSets() != null) {
                    RoutineDetails routineDetails = getRoutineDetails(updateSet.getRoutineDetailsId());
                    for (RoutineSetsDto newSet : updateSet.getNewSets()) {
                        routinesSets(routineDetails, newSet);
                        routineDetails.updateTotalSets(routineDetails.getTotalSets() + 1);
                    }
                }
            }
        }

        // 삭제할 세트수 존재시
        if (deleteSets != null) {
            for (DeleteSets deleteSet : deleteSets) {
                Long routineDetailsId = deleteSet.getRoutineDetailsId();
                RoutineDetails routineDetails = getRoutineDetails(routineDetailsId);
                for (Long setsId : deleteSet.getSetsIds()) {
                    routineSetsRepository.deleteByIdAndRoutineDetailsId(setsId, routineDetailsId);
                    routineDetails.updateTotalSets(routineDetails.getTotalSets() - 1);
                }
            }
        }

        // 새로운 운동 목록 존재시
        if (newExercises != null) {
            for (ExerciseDto newExercise : newExercises) {
                RoutineDetails newRoutineDetails;

                if (newExercise.getSource().equals(DEFAULT)) {
                    Exercise exercise = getExercise(newExercise.getExerciseId());       // 운동 객체 조회
                    newRoutineDetails = RoutineDetails.of(exercise, routine);
                } else {
                    CustomExercise customExercise = getCustomExercise(newExercise.getExerciseId());
                    newRoutineDetails = RoutineDetails.of(customExercise, routine);
                }
                RoutineDetails routineDetails = routineDetailsRepository.save(newRoutineDetails);

                // 새로운 루틴 세트 등록
                for (RoutineSetsDto set : newExercise.getRoutineSets()) {
                    routinesSets(routineDetails, set);
                }
            }
        }

        // 대채 운동 존재시
        if (changeExercises != null) {
            for (ChangeExercise changeExercise : changeExercises) {
                RoutineDetails routineDetails = getRoutineDetails(changeExercise.getRoutineDetailsId());

                String source = changeExercise.getSource();
                Long exerciseId = changeExercise.getExerciseId();
                Object exercise;
                if (source.equals(DEFAULT)){      // 기본 운동 일시
                    exercise = getExercise(exerciseId);
                }else                               // 커스텀 운동 일시
                    exercise = getCustomExercise(exerciseId);

                routineDetails.updateExercise(exercise);
            }
        }

        // 운동 순서 변경시
        if (changeOrders!=null){
            for (ChangeOrders changeOrder : changeOrders) {
                RoutineDetails routineDetails = getRoutineDetails(changeOrder.getRoutineDetailsId());
                routineDetails.updateOrders(changeOrder.getOrders());
            }
        }

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

    /**
     * 루틴별로 세트수를 추가할떄 사용되는 메서드
     * 운동 타입별로 운동 세트 저장 상이
     */
    private void routinesSets(RoutineDetails routineDetails, RoutineSetsDto newSet) {
        Integer sets = newSet.getSets();        // 세트수
        Integer kg = newSet.getKg();            // kg
        Integer times = newSet.getTimes();      // 시간수
        RoutineSets routineSets;
        if (sets != null & kg != null) {        // 무게-세트 기준
            routineSets = RoutineSets.ofWeightAndSets(kg,sets, routineDetails);
        } else if (times != null) {             // 시간 기준
            routineSets = RoutineSets.ofTimes(times, routineDetails);
        } else                                  // 세트 기준
            routineSets = RoutineSets.ofSets(sets, routineDetails);
        routineSetsRepository.save(routineSets);
    }

    /**
     * 상세 루틴의 정보를 조회
     */
    private RoutineDetails getRoutineDetails(Long  routineDetailsId) {
        return routineDetailsRepository.findById(routineDetailsId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
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
