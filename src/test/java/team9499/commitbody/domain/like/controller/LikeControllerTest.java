package team9499.commitbody.domain.like.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import team9499.commitbody.config.SecurityTestConfig;
import team9499.commitbody.domain.article.dto.response.ArticleCountResponse;
import team9499.commitbody.domain.article.event.ElsArticleCountEvent;
import team9499.commitbody.domain.like.service.LikeService;
import team9499.commitbody.global.aop.AspectAdvice;
import team9499.commitbody.global.aop.Pointcuts;
import team9499.commitbody.mock.MockUser;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@Import({SecurityTestConfig.class, Pointcuts.class, AspectAdvice.class})
@ImportAutoConfiguration(AopAutoConfiguration.class)
@WebMvcTest(LikeController.class)
class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private LikeService likeService;
    @MockBean
    private ApplicationEventPublisher eventPublisher;

    private final ObjectMapper ob = new ObjectMapper();

    @DisplayName("운동 댓글 좋아요")
    @MockUser
    @Test
    void ExerciseCommentLike() throws Exception {
        Map<String, Long> request = Map.of("exCommentId", 1L);
        given(likeService.exerciseCommentLike(anyLong(), anyLong())).willReturn("등록");

        mockMvc.perform(post("/api/v1/comment-exercise/like")
                        .with(csrf())
                        .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("등록"));
    }

    @DisplayName("게시글 좋아요")
    @MockUser
    @Test
    void articleLike() throws Exception {
        Map<String, Long> request = Map.of("articleId", 1L);
        ArticleCountResponse response = new ArticleCountResponse();
        response.setArticleId(1L);
        response.setCount(1);
        response.setType("등록");

        given(likeService.articleLike(anyLong(), anyLong())).willReturn(response);
        eventPublisher.publishEvent(new ElsArticleCountEvent(1L, 1, "좋아요"));

        mockMvc.perform(post("/api/v1/article/like")
                        .with(csrf())
                        .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("등록"));

        verify(eventPublisher, times(1)).publishEvent(any(ElsArticleCountEvent.class));
    }

    @DisplayName("대/댓글 좋아요")
    @MockUser
    @Test
    void articleCommentLike() throws Exception {
        Map<String, Long> request = Map.of("commentId", 1L);
        given(likeService.articleCommentLike(anyLong(), anyLong())).willReturn("등록");

        mockMvc.perform(post("/api/v1/article/comment/like")
                        .with(csrf())
                        .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("등록"));
    }


}