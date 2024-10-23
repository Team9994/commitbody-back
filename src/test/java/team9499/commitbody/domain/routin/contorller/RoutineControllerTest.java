package team9499.commitbody.domain.routin.contorller;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import team9499.commitbody.config.SecurityTestConfig;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.routin.dto.RoutineDto;
import team9499.commitbody.domain.routin.dto.response.MyRoutineResponse;
import team9499.commitbody.domain.routin.dto.rqeust.EditRoutineRequest;
import team9499.commitbody.domain.routin.dto.rqeust.RoutineExercise;
import team9499.commitbody.domain.routin.dto.rqeust.RoutineRequest;
import team9499.commitbody.domain.routin.service.RoutineService;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.aop.AspectAdvice;
import team9499.commitbody.global.aop.Pointcuts;
import team9499.commitbody.mock.MockUser;

@ExtendWith(MockitoExtension.class)
@Import({SecurityTestConfig.class, Pointcuts.class, AspectAdvice.class})
@WebMvcTest(RoutineController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
class RoutineControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private RoutineService routineService;

    private final ObjectMapper ob = new ObjectMapper();

    private List<RoutineExercise> exercises = new LinkedList<>();
    private String exerciseName;

    @BeforeEach
    void init(){
        exercises.add(new RoutineExercise(1L,1,"default"));
        exercises.add(new RoutineExercise(2L,2,"default"));
        exercises.add(new RoutineExercise(3L,3,"default"));

        exerciseName = "루틴 1";
    }
    
    @DisplayName("루틴 등록")
    @MockUser
    @Test
    void addRoutine() throws Exception{
        RoutineRequest request = new RoutineRequest();
        request.setRoutineName(exerciseName);
        request.setRoutineExercises(exercises);

        doNothing().when(routineService).saveRoutine(anyLong(),anyList(),anyString());

        mockMvc.perform(post("/api/v1/routine")
                .with(csrf())
                .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("루틴 등록 성공"));
    }


    @DisplayName("루틴 조횐")
    @MockUser
    @Test
    void getRoutine() throws Exception{
        List<Object> objects = new ArrayList<>();
        objects.add(new Exercise(1L,"등 운동","URL", ExerciseTarget.등, ExerciseType.WEIGHT_AND_REPS, ExerciseEquipment.BARBELL,1.1F,new ArrayList<>()));
        objects.add(new Exercise(2L,"가슴 운동","URL", ExerciseTarget.가슴, ExerciseType.WEIGHT_AND_REPS, ExerciseEquipment.BARBELL,1.1F,new ArrayList<>()));
        List<RoutineDto> routineDtos = new ArrayList<>();
        routineDtos.add(new RoutineDto(1L,"루틴1", Set.of("등","가슴"),objects));
        MyRoutineResponse response = new MyRoutineResponse();
        response.setRoutineDtos(routineDtos);

        given(routineService.getMyRoutine(anyLong())).willReturn(response);

        mockMvc.perform(get("/api/v1/routine")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routineDtos.size()").value(1))
                .andExpect(jsonPath("$.data.routineDtos[0].routineId").value(1))
                .andExpect(jsonPath("$.data.routineDtos[0].exercises[0].exerciseName").value("등 운동"))
                .andExpect(jsonPath("$.data.routineDtos[0].exercises[1].exerciseName").value("가슴 운동"))
                .andDo(print());
    }
    

    @DisplayName("루틴 편집")
    @MockUser
    @Test
    void updateRoutine() throws Exception{
        List<ExerciseDto> exerciseDtos = new ArrayList<>();
        exerciseDtos.add(ExerciseDto.of(1L,1L,"등 운동","URL",4,"무게와 횟수",1));
        exerciseDtos.add(ExerciseDto.of(1L,2L,"가슴 운동","URL",4,"횟수",2));
        exerciseDtos.add(ExerciseDto.of(1L,3L,"어깨 운동","URL",4,"시간 단위",3));
        EditRoutineRequest request = new EditRoutineRequest();
        request.setRoutineName("변경한 루틴 명");
        request.setExercises(exerciseDtos);

        doNothing().when(routineService).updateRoutine(anyLong(),anyLong(),anyString(),anyList());

        mockMvc.perform(put("/api/v1/routine/1")
                .with(csrf())
                .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("루틴 수정 완료"))
                .andDo(print());

        verify(routineService,times(1)).updateRoutine(anyLong(),anyLong(),anyString(),anyList());
    }
    
    @DisplayName("루틴 삭제")
    @MockUser
    @Test
    void deleteRoutine() throws Exception {
        doNothing().when(routineService).deleteRoutine(anyLong(),anyLong());

        mockMvc.perform(delete("/api/v1/routine/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("삭제 성공"));
    }
    
    @DisplayName("작성자가 아닐시 예외 발생")
    @MockUser
    @Test
    void onlyWriterUseByException()throws Exception {
        doThrow(new InvalidUsageException(ExceptionStatus.BAD_REQUEST, ExceptionType.AUTHOR_ONLY)).when(routineService).deleteRoutine(anyLong(),anyLong());

        mockMvc.perform(delete("/api/v1/routine/1")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("작성자만 이용할 수 있습니다."));
    }
}