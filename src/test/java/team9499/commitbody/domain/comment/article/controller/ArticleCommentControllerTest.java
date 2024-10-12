package team9499.commitbody.domain.comment.article.controller;

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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import team9499.commitbody.config.SecurityTestConfig;
import team9499.commitbody.domain.article.dto.response.ArticleCountResponse;
import team9499.commitbody.domain.article.event.ElsArticleCountEvent;
import team9499.commitbody.domain.comment.article.domain.OrderType;
import team9499.commitbody.domain.comment.article.dto.ArticleCommentDto;
import team9499.commitbody.domain.comment.article.dto.request.SaveArticleCommentRequest;
import team9499.commitbody.domain.comment.article.dto.response.ArticleCommentResponse;
import team9499.commitbody.domain.comment.article.service.ArticleCommentService;
import team9499.commitbody.global.aop.AspectAdvice;
import team9499.commitbody.global.aop.Pointcuts;
import team9499.commitbody.mock.MockUser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Import({SecurityTestConfig.class, Pointcuts.class, AspectAdvice.class})
@WebMvcTest(ArticleCommentController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
class ArticleCommentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ArticleCommentService articleCommentService;
    @MockBean private ApplicationEventPublisher eventPublisher;

    private final ObjectMapper ob = new ObjectMapper();

    @DisplayName("게시글 댓글 등록")
    @MockUser
    @Test
    void saveArticleCommentAPI() throws Exception{

        SaveArticleCommentRequest request = new SaveArticleCommentRequest();
        request.setArticleId(1L);
        request.setContent("댓글");
        request.setParentId(null);
        request.setReplyNickname("알림 대상자");

        ArticleCountResponse response = ArticleCountResponse.of(1L, null, "댓글 작성 성공");

        eventPublisher.publishEvent(new ElsArticleCountEvent());

        given(articleCommentService.saveArticleComment(anyLong(),anyLong(),isNull(),anyString(),anyString())).willReturn(response);

        mockMvc.perform(post("/api/v1/article/comment")
                        .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("댓글 작성 성공"));

        verify(eventPublisher,times(1)).publishEvent(any(ElsArticleCountEvent.class));
    }

    @DisplayName("게시글 대댓글 등록")
    @MockUser
    @Test
    void saveArticleReplyCommentAPI() throws Exception{

        SaveArticleCommentRequest request = new SaveArticleCommentRequest();
        request.setArticleId(1L);
        request.setContent("댓글");
        request.setParentId(2L);
        request.setReplyNickname("알림 대상자");

        ArticleCountResponse response = ArticleCountResponse.of(1L, null, "대댓글 작성 성공");

        given(articleCommentService.saveArticleComment(anyLong(),anyLong(),isNotNull(),anyString(),anyString())).willReturn(response);

        mockMvc.perform(post("/api/v1/article/comment")
                .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("대댓글 작성 성공"));

        verify(eventPublisher,times(0)).publishEvent(any(ElsArticleCountEvent.class));
    }

    
    @DisplayName("대/댓글 수정")
    @MockUser
    @Test
    void updateArticleComment() throws Exception{
        Map<String,String> request = Map.of("content","수정한 댓글");
        doNothing().when(articleCommentService).updateArticleComment(anyLong(),anyLong(),anyString());

        mockMvc.perform(put("/api/v1/article/comment/1")
                .content(ob.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("댓글 수정 성공"));
    }


    @DisplayName("댓글 삭제")
    @MockUser
    @Test
    void deleteArticleComment() throws Exception{
        ArticleCountResponse response = ArticleCountResponse.of(1L, 20, null);
        given(articleCommentService.deleteArticleComment(anyLong(),anyLong())).willReturn(response);
        eventPublisher.publishEvent(new ElsArticleCountEvent(response.getArticleId(),response.getCount(),"댓글"));

        mockMvc.perform(delete("/api/v1/article/comment/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("삭제 성공"));

        verify(eventPublisher,times(1)).publishEvent(any(ElsArticleCountEvent.class));
    }

    @DisplayName("대댓글 삭제")
    @MockUser
    @Test
    void deleteArticleReplyComment() throws Exception{
        given(articleCommentService.deleteArticleComment(anyLong(),anyLong())).willReturn(null);

        mockMvc.perform(delete("/api/v1/article/comment/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("삭제 성공"));

        verify(eventPublisher,times(0)).publishEvent(any(ElsArticleCountEvent.class));
    }
    
    @DisplayName("조회")
    @MockUser
    @Test
    void getAllByArticleComment() throws Exception {
        List<ArticleCommentDto> articleCommentDtoList = new ArrayList<>();
        articleCommentDtoList.add(ArticleCommentDto.builder().commentId(1L).build());
        articleCommentDtoList.add(ArticleCommentDto.builder().commentId(2L).build());
        articleCommentDtoList.add(ArticleCommentDto.builder().commentId(3L).build());
        ArticleCommentResponse articleCommentResponse = new ArticleCommentResponse(articleCommentDtoList.size(),false,articleCommentDtoList);

        given(articleCommentService.getComments(anyLong(),anyLong(),isNull(),isNull(),eq(OrderType.RECENT),any(Pageable.class))).willReturn(articleCommentResponse);


        mockMvc.perform(get("/api/v1/article/1/comment")
                        .param("sortOrder","RECENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("댓글 조회"))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.comments.size()").value(3))
                .andExpect(jsonPath("$.data.comments").isArray());
    }
}