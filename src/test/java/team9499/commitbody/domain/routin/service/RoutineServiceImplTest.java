package team9499.commitbody.domain.routin.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.routin.domain.Routine;
import team9499.commitbody.domain.routin.domain.RoutineDetails;
import team9499.commitbody.domain.routin.dto.response.MyRoutineResponse;
import team9499.commitbody.domain.routin.dto.rqeust.RoutineExercise;
import team9499.commitbody.domain.routin.repository.RoutineDetailsRepository;
import team9499.commitbody.domain.routin.repository.RoutineRepository;
import team9499.commitbody.global.redis.RedisService;

@Slf4j
@ExtendWith(MockitoExtension.class)
class RoutineServiceImplTest {

    @Mock private RoutineRepository routineRepository;
    @Mock private RoutineDetailsRepository routineDetailsRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private RoutineBatchService routineBatchService;
    @Mock private RedisService redisService;

    @InjectMocks private RoutineServiceImpl routineService;

    private Member member;
    private Routine routine;
    private Exercise exercise1;
    private Exercise exercise2;
    private Exercise exercise3;
    private List<RoutineDetails> routineDetails;

    @BeforeEach
    void init(){
        member = Member.builder().id(1L).nickname("닉네임").isWithdrawn(false).weight(78).height(178).build();
        routine = Routine.create(member,"루틴 1");
        exercise1 = new Exercise(1L,"운동 1","주소", ExerciseTarget.등, ExerciseType.WEIGHT_AND_REPS, ExerciseEquipment.BAND,2.5f,new ArrayList<>());
        exercise2 = new Exercise(2L,"운동 2","주소", ExerciseTarget.등, ExerciseType.REPS_ONLY, ExerciseEquipment.BAND,2.5f,new ArrayList<>());
        exercise3 = new Exercise(3L,"운동 3","주소", ExerciseTarget.등, ExerciseType.TIME_ONLY, ExerciseEquipment.BAND,2.5f,new ArrayList<>());

        routineDetails = Arrays.asList(
                RoutineDetails.of(exercise1,routine,1),
                RoutineDetails.of(exercise2,routine,3),
                RoutineDetails.of(exercise3,routine,2)
        );
        routine.setId(1L);
        routine.setList(routineDetails);

    }

    @DisplayName("운동 루틴 저장")
    @Test
    void saveRoutine(){
        List<RoutineExercise> routineExercises = Arrays.asList(
                new RoutineExercise(1L,1,"default"),
                new RoutineExercise(2L,3,"default"),
                new RoutineExercise(3L,2,"default")
        );
        when(redisService.getMemberDto(anyString())).thenReturn(Optional.of(member));
        when(routineRepository.save(any())).thenReturn(routine);
        when(exerciseRepository.findById(any()))
                .thenReturn(Optional.of(exercise1))
                .thenReturn(Optional.of(exercise2))
                .thenReturn(Optional.of(exercise3));
        when(routineDetailsRepository.saveAll(any())).thenReturn(routineDetails);

        routineService.saveRoutine(member.getId(),routineExercises,"루틴 1");

        verify(routineRepository,times(1)).save(any());
        verify(routineDetailsRepository,times(1)).saveAll(any());
        verify(exerciseRepository,times(3)).findById(any());
    }


    @DisplayName("나의 루틴 전체 조회")
    @Test
    void getMyRoutine(){
        List<Routine> routines = new ArrayList<>();
        routines.add(Routine.builder().id(1L).routineName("루틴1").member(member).list(routineDetails).build());
        routines.add(Routine.builder().id(1L).routineName("루틴2").member(member).list(routineDetails).build());
        routines.add(Routine.builder().id(1L).routineName("루틴3").member(member).list(routineDetails).build());

        when(routineRepository.findAllByMemberId(any())).thenReturn(routines);

        MyRoutineResponse myRoutine = routineService.getMyRoutine(member.getId());

        assertThat(myRoutine.getRoutineDtos().size()).isEqualTo(3);
        assertThat(myRoutine.getRoutineDtos().get(0).getRoutineName()).isEqualTo("루틴1");
        assertThat(myRoutine.getRoutineDtos().get(1).getRoutineName()).isEqualTo("루틴2");
        assertThat(myRoutine.getRoutineDtos().get(2).getRoutineName()).isEqualTo("루틴3");
    }
    
    @DisplayName("루틴 상세 조회")
    @Test
    void getDetailRoutine(){
        when(routineRepository.findByIdAndMemberId(eq(routine.getId()),eq(member.getId()))).thenReturn(Optional.of(routine));

        MyRoutineResponse detailMyRoutine = routineService.getDetailMyRoutine(member.getId(), routine.getId());

        assertThat(detailMyRoutine.getRoutineDtos().size()).isEqualTo(1);
        assertThat(detailMyRoutine.getRoutineDtos().get(0).getRoutineName()).isEqualTo("루틴 1");
    }

    @DisplayName("루틴 업데이트")
    @Test
    void updateRoutine(){
        List<ExerciseDto> exerciseDtos = new ArrayList<>();
        exerciseDtos.add(ExerciseDto.builder().source("default").exerciseId(1L).build());
        exerciseDtos.add(ExerciseDto.builder().source("default").exerciseId(2L).build());
        exerciseDtos.add(ExerciseDto.builder().source("default").exerciseId(3L).build());

        when(routineRepository.findByIdAndMemberId(anyLong(),anyLong())).thenReturn(Optional.of(routine));
        doNothing().when(routineDetailsRepository).deleteAllByInQuery(eq(routineDetails.stream().map(RoutineDetails::getId).toList()));
        doNothing().when(routineBatchService).saveRoutineDetailInBatch(anyList());
        when(exerciseRepository.findById(anyLong())).thenReturn(Optional.of(exercise1)).thenReturn(Optional.of(exercise2)).thenReturn(Optional.of(exercise3));

        routineService.updateRoutine(routine.getId(),member.getId(),"새로운 루틴",exerciseDtos);

        assertThat(routine.getRoutineName()).isEqualTo("새로운 루틴");
        assertThat(routine.getRoutineName()).isNotEqualTo("루틴 1");
        verify(exerciseRepository,times(3)).findById(anyLong());
    }
    
    @DisplayName("루틴 삭제")
    @Test
    void deleteRoutine(){
        when(routineRepository.findByIdAndMemberId(anyLong(),anyLong())).thenReturn(Optional.of(routine));
        doNothing().when(routineRepository).deleteRoutine(eq(routine.getId()));

        routineService.deleteRoutine(routine.getId(),member.getId());

        verify(routineRepository,times(1)).deleteRoutine(anyLong());
    }

}