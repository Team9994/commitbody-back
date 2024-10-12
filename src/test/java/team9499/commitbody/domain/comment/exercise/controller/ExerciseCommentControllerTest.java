package team9499.commitbody.domain.comment.exercise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import team9499.commitbody.config.SecurityTestConfig;
import team9499.commitbody.domain.comment.exercise.dto.ExerciseCommentDto;
import team9499.commitbody.domain.comment.exercise.dto.request.ExerciseCommentRequest;
import team9499.commitbody.domain.comment.exercise.dto.request.UpdateExerciseCommentRequest;
import team9499.commitbody.domain.comment.exercise.dto.response.ExerciseCommentResponse;
import team9499.commitbody.domain.comment.exercise.service.ExerciseCommentService;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.aop.AspectAdvice;
import team9499.commitbody.global.aop.Pointcuts;
import team9499.commitbody.mock.MockUser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Import({SecurityTestConfig.class, Pointcuts.class, AspectAdvice.class})
@WebMvcTest(ExerciseCommentController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
class ExerciseCommentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ExerciseCommentService exerciseCommentService;

    private ObjectMapper ob = new ObjectMapper();


    @DisplayName("댓글 저장 API")
    @MockUser
    @Test
    void saveExerciseComment() throws Exception {
        ExerciseCommentRequest request = new ExerciseCommentRequest();
        request.setExerciseId(1L);
        request.setContent("댓글");
        request.setSource("default");

        doNothing().when(exerciseCommentService).saveExerciseComment(anyLong(),anyLong(),eq("default"),anyString());

        mockMvc.perform(post("/api/v1/comment-exercise")
                        .with(csrf())
                        .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("등록 성공"));
    }
    
    
    @DisplayName("운동 댓글 조회")
    @MockUser
    @Test
    void getExerciseComments() throws Exception{
        List<ExerciseCommentDto> commentDtoList = new ArrayList<>();
        commentDtoList.add(ExerciseCommentDto.of(1L,"닉네임1","내용1","1분전",true,10,false));
        commentDtoList.add(ExerciseCommentDto.of(2L,"닉네임2","내용2","2분전",false,10,true));
        commentDtoList.add(ExerciseCommentDto.of(3L,"닉네임3","내용3","3분전",true,10,false));

        ExerciseCommentResponse response = new ExerciseCommentResponse();
        response.setCommentList(commentDtoList);
        response.setHasNext(false);

        given(exerciseCommentService.getExerciseComments(anyLong(),anyLong(),anyString(),any(Pageable.class),isNull())).willReturn(response);

        mockMvc.perform(get("/api/v1/comment-exercise/1")
                .param("source","default"))
                .andExpect(jsonPath("$.data.commentList.size()").value(3))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }
    
    @DisplayName("운동 댓글 삭제")
    @MockUser
    @Test
    void deleteExerciseComment() throws Exception{
        doNothing().when(exerciseCommentService).deleteExerciseComment(anyLong(),anyLong());

        mockMvc.perform(delete("/api/v1/comment-exercise/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("삭제 성공"));
    }

    @DisplayName("운동 댓글 수정")
    @MockUser
    @Test
    void updateExerciseComment() throws Exception{
        UpdateExerciseCommentRequest request = new UpdateExerciseCommentRequest();
        request.setExerciseCommentId(1L);
        request.setContent("변경 댓글");
        
        doNothing().when(exerciseCommentService).updateExerciseComment(anyLong(),anyLong(),anyString());

        mockMvc.perform(put("/api/v1/comment-exercise")
                        .with(csrf())
                        .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("업데이트 성공"));

    }

    @DisplayName("작상자 아닐시 예외")
    @MockUser
    @Test
    void notOnlyWriter() throws Exception {
        UpdateExerciseCommentRequest request = new UpdateExerciseCommentRequest();
        request.setExerciseCommentId(1L);
        request.setContent("변경 댓글");

        doThrow(new InvalidUsageException(ExceptionStatus.BAD_REQUEST, ExceptionType.AUTHOR_ONLY)).when(exerciseCommentService).updateExerciseComment(anyLong(), anyLong(), anyString());

        mockMvc.perform(put("/api/v1/comment-exercise")
                        .with(csrf())
                        .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("작성자만 이용할 수 있습니다."));
    }

}