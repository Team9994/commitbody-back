package team9499.commitbody.domain.article.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import team9499.commitbody.config.SecurityTestConfig;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.article.dto.request.ArticleRequest;
import team9499.commitbody.domain.article.dto.response.AllArticleResponse;
import team9499.commitbody.domain.article.dto.response.ProfileArticleResponse;
import team9499.commitbody.domain.article.event.ElsArticleEvent;
import team9499.commitbody.domain.article.service.ArticleService;
import team9499.commitbody.domain.article.service.ElsArticleService;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.InvalidUsageException;
import team9499.commitbody.global.aop.AspectAdvice;
import team9499.commitbody.global.aop.Pointcuts;
import team9499.commitbody.global.redis.RedisService;
import team9499.commitbody.mock.MockUser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static team9499.commitbody.domain.article.domain.ArticleCategory.*;
import static team9499.commitbody.domain.article.domain.ArticleType.*;
import static team9499.commitbody.domain.article.domain.Visibility.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@Import({SecurityTestConfig.class, Pointcuts.class, AspectAdvice.class})
@WebMvcTest(ArticleController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
class ArticleControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ArticleService articleService;
    @MockBean private ApplicationEventPublisher eventPublisher;
    @MockBean private ElsArticleService elsArticleService;
    @MockBean private RedisService redisService;

    private final ObjectMapper ob = new ObjectMapper();
    private Long memberId = 1L;
    private Member member;
    private Member otherMember;
    @BeforeEach
    void init(){
        member = Member.builder().id(memberId).loginType(LoginType.KAKAO).nickname("테스트 닉네임").socialId("test").build();
        otherMember = Member.builder().id(2L).loginType(LoginType.KAKAO).nickname("상대방 닉네임").socialId("test").build();
    }

    @DisplayName("운동 게시글 조회 - 운동 인증")
    @MockUser
    @Test
    void getArticleExercise() throws Exception {

        List<ArticleDto> articleDtoList = new ArrayList<>();
        articleDtoList.add(ArticleDto.of(Article.of("1번","1번내용", EXERCISE, null, PUBLIC,member),member,"testurl-1"));
        articleDtoList.add(ArticleDto.of(Article.of("2번","2번내용", EXERCISE, null, PUBLIC,member),member,"testurl-2"));
        articleDtoList.add(ArticleDto.of(Article.of("3번","3번내용", EXERCISE, null, PUBLIC,member),member,"testurl-3"));

        AllArticleResponse allArticleResponse = new AllArticleResponse();
        allArticleResponse.setArticles(articleDtoList);
        allArticleResponse.setTotalCount(articleDtoList.size());
        allArticleResponse.setHasNext(false);

        given(articleService.getAllArticles(eq(memberId),eq(EXERCISE),eq(ALL),isNull(),any(Pageable.class))).willReturn(allArticleResponse);

        mockMvc.perform(get("/api/v1/article")
                        .param("type","EXERCISE")
                        .param("category","ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.size()").value(3))
                .andExpect(jsonPath("$.data.totalCount").value(3));
    }

    @DisplayName("운동 게시글 조회 - 운동 질문")
    @Test
    @MockUser()
    void getArticleInfo() throws Exception {
        List<ArticleDto> articleDtoList = new ArrayList<>();
        articleDtoList.add(ArticleDto.of(Article.of("1번","1번내용", INFO_QUESTION, FEEDBACK, PUBLIC,member),member,"testurl"));

        AllArticleResponse allArticleResponse = new AllArticleResponse();
        allArticleResponse.setArticles(articleDtoList);
        allArticleResponse.setTotalCount(1);
        allArticleResponse.setHasNext(false);

        given(articleService.getAllArticles(eq(memberId),eq(INFO_QUESTION),eq(FEEDBACK),isNull(),any(Pageable.class))).willReturn(allArticleResponse);

        mockMvc.perform(get("/api/v1/article")
                        .param("type","INFO_QUESTION")
                        .param("category","FEEDBACK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articles.[0].content").value("1번내용"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }
    
    @DisplayName("운동 인증 게시글 등록")
    @MockUser
    @Test
    void exerciseArticleSave() throws Exception {
        ArticleRequest articleRequest = new ArticleRequest();
        articleRequest.setArticleType(EXERCISE);
        articleRequest.setContent("운동은 힘들다");
        articleRequest.setVisibility(PUBLIC);
        articleRequest.setArticleCategory(null);

        ArticleDto articleDto = ArticleDto.of(Article.of(null, "운동은 힘들다", EXERCISE, null, PUBLIC, member), member, "testfile");
        articleDto.setArticleId(1L);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "testfile", "image/jpg", "test".getBytes());
        MockMultipartFile request = new MockMultipartFile("articleSaveRequest", null, "application/json",ob.writeValueAsString(articleRequest).getBytes(StandardCharsets.UTF_8));
        given(articleService.saveArticle(eq(memberId),isNull(),anyString(),eq(EXERCISE),isNull(),any(),eq(mockMultipartFile))).willReturn(articleDto);

        mockMvc.perform(multipart("/api/v1/article")
                        .file(mockMultipartFile)
                        .file(request)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("등록 성공"))
                .andExpect(jsonPath("$.data").value(1));
    }

    @DisplayName("몸평 게시글외 동영상 등록시 예외 발생")
    @MockUser
    @Test
    void exerciseArticleSaveThrowVideo() throws Exception {
        ArticleRequest articleRequest = new ArticleRequest();
        articleRequest.setArticleType(EXERCISE);
        articleRequest.setContent("운동은 힘들다");
        articleRequest.setVisibility(PUBLIC);
        articleRequest.setArticleCategory(null);

        ArticleDto articleDto = ArticleDto.of(Article.of(null, "운동은 힘들다", EXERCISE, null, PUBLIC, member), member, "testfile");
        articleDto.setArticleId(1L);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "testfile", "video/mp4", "test".getBytes());
        MockMultipartFile request = new MockMultipartFile("articleSaveRequest", null, "application/json",ob.writeValueAsString(articleRequest).getBytes(StandardCharsets.UTF_8));
        given(articleService.saveArticle(eq(memberId),isNull(),anyString(),eq(EXERCISE),isNull(),any(),eq(mockMultipartFile))).willThrow(new InvalidUsageException(ExceptionStatus.BAD_REQUEST, ExceptionType.ONLY_IMAGE));

        mockMvc.perform(multipart("/api/v1/article")
                        .file(mockMultipartFile)
                        .file(request)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미지 파일만 등록 가능합니다."));
    }

    @DisplayName("BindingResult 테스트")
    @MockUser
    @Test
    void infoArticleEvent() throws Exception {
        // given
        ArticleRequest articleRequest = new ArticleRequest();
        articleRequest.setTitle(null);
        articleRequest.setArticleType(INFO_QUESTION);
        articleRequest.setArticleCategory(null);
        articleRequest.setContent("몸평 게시글");
        articleRequest.setVisibility(null);

        ArticleDto articleDto = ArticleDto.of(Article.of("몸평 질문", "몸평 게시글", INFO_QUESTION, BODY_REVIEW, PUBLIC, member), member, "testfile.mp4");
        articleDto.setArticleId(1L);


        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "testfile", "video/mp4", "test".getBytes());
        MockMultipartFile request = new MockMultipartFile("articleSaveRequest", null, "application/json", ob.writeValueAsString(articleRequest).getBytes(StandardCharsets.UTF_8));

        given(articleService.saveArticle(eq(memberId), isNull(), anyString(), eq(INFO_QUESTION), eq(BODY_REVIEW), eq(PUBLIC), eq(mockMultipartFile)))
                .willReturn(articleDto);

        mockMvc.perform(multipart("/api/v1/article")
                        .file(mockMultipartFile)
                        .file(request)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.visibility").value("공개범위를 선택해주세요"));
    }

    @DisplayName("게시글 상세 조회 API")
    @MockUser
    @Test
    void detailsArticle() throws Exception {
        ArticleDto articleDto = ArticleDto.of(Article.of("몸평 질문", "몸평 게시글", INFO_QUESTION, BODY_REVIEW, PUBLIC, member), member, "testfile.mp4");
        articleDto.setArticleId(1L);

        given(articleService.getDetailArticle(eq(memberId),eq(1L))).willReturn(articleDto);

        mockMvc.perform(get("/api/v1/article/"+1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("몸평 질문"))
                .andExpect(jsonPath("$.data.member.nickname").value("테스트 닉네임"));
    }
    
    @DisplayName("프로필 게시글 조회 - 운동인증")
    @MockUser
    @Test
    void profileArticleExercise() throws Exception {

        List<ArticleDto> articleDtoList = new ArrayList<>();
        articleDtoList.add(ArticleDto.of(Article.of(null, "운동 인증1", EXERCISE, null, PUBLIC, member), member, "testfile.mp4"));
        articleDtoList.add(ArticleDto.of(Article.of(null, "운동 인증2", EXERCISE, null, PUBLIC, member), member, "testfile.mp4"));
        articleDtoList.add(ArticleDto.of(Article.of(null, "운동 인증3", EXERCISE, null, PUBLIC, member), member, "testfile.mp4"));


        ProfileArticleResponse profileArticleResponse = new ProfileArticleResponse();
        profileArticleResponse.setHasNext(false);
        profileArticleResponse.setArticles(articleDtoList);

        given(articleService.getAllProfileArticle(eq(memberId),eq(memberId),eq(EXERCISE),isNull(),any(Pageable.class))).willReturn(profileArticleResponse);

        mockMvc.perform(get("/api/v1/my-page/articles/"+1)
                        .param("type","EXERCISE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.articles.size()").value(3))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.articles.[0].content").value("운동 인증1"));
    }

    @DisplayName("프로필 게시글 조회 - 상대방 프로필 조회")
    @MockUser
    @Test
    void profileArticleInfo() throws Exception {

        List<ArticleDto> articleDtoList = new ArrayList<>();
        articleDtoList.add(ArticleDto.of(Article.of("몸평 게시글", "몸평", INFO_QUESTION, BODY_REVIEW, PUBLIC, member), otherMember, "testfile.mp4"));
        articleDtoList.add(ArticleDto.of(Article.of("질문 게시글", "질문", INFO_QUESTION, INFORMATION, PRIVATE, member), otherMember, "testfile.mp4"));


        ProfileArticleResponse profileArticleResponse = new ProfileArticleResponse();
        profileArticleResponse.setHasNext(false);
        profileArticleResponse.setArticles(articleDtoList);

        given(articleService.getAllProfileArticle(eq(memberId),eq(2L),eq(INFO_QUESTION),isNull(),any(Pageable.class))).willReturn(profileArticleResponse);

        mockMvc.perform(get("/api/v1/my-page/articles/"+2)
                        .param("type","INFO_QUESTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articles.size()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }
    
    
    @DisplayName("게시글 수정")
    @MockUser
    @Test
    void articleUpdate() throws Exception {

        ArticleRequest articleRequest = new ArticleRequest();
        articleRequest.setTitle("변경된 게시글");
        articleRequest.setArticleType(INFO_QUESTION);
        articleRequest.setArticleCategory(FEEDBACK);
        articleRequest.setContent("변경된 내용");
        articleRequest.setVisibility(PUBLIC);

        ArticleDto articleDto = ArticleDto.of(Article.of("변경된 게시글", "변경된 내용", INFO_QUESTION, FEEDBACK, PUBLIC, member), member, "testfile.jpeg");
        articleDto.setArticleId(1L);

        ElsArticleEvent elsArticleEvent = new ElsArticleEvent(articleDto, "수정");
        eventPublisher.publishEvent(elsArticleEvent);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "testfile", "image/jpeg", "test".getBytes());
        MockMultipartFile request = new MockMultipartFile("updateArticleRequest", null, "application/json", ob.writeValueAsString(articleRequest).getBytes(StandardCharsets.UTF_8));

        given(articleService.updateArticle(eq(memberId),eq(1L),eq("변경된 게시글"),eq("변경된 내용"),eq(INFO_QUESTION),eq(FEEDBACK),eq(PUBLIC),any())).willReturn(articleDto);

        mockMvc.perform(multipart("/api/v1/article/1")
                .file(mockMultipartFile)
                .file(request)
                        .with(r -> {
                                    r.setMethod("PUT");
                                    return r;
                                }
                        )
                        .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("수정 성공"));
        
        verify(eventPublisher,times(1)).publishEvent(eq(new ElsArticleEvent(articleDto,"수정"))); // 수정 이벤트가 발생하는지 테스트

    }

    @DisplayName("게시글 삭제")
    @MockUser
    @Test
    void deleteArticle() throws Exception {
        ArticleDto articleDto = ArticleDto.of(Article.of("삭제 게시글", "삭제게시글", INFO_QUESTION, FEEDBACK, PUBLIC, member), member, "testfile.jpeg");
        articleDto.setArticleId(1L);

        given(articleService.deleteArticle(eq(memberId),eq(1L))).willReturn(articleDto);
        ElsArticleEvent elsArticleEvent = new ElsArticleEvent(articleDto, "삭제");
        eventPublisher.publishEvent(elsArticleEvent);

        mockMvc.perform(delete("/api/v1/article/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("삭제 성공"));

        verify(eventPublisher).publishEvent(eq(elsArticleEvent));
    }
    
    @DisplayName("수정/삭제 이용시 작성자가 아닐시 403 예외 발생")
    @MockUser
    @Test
    void onlyWriterUse() throws Exception {

        given(articleService.deleteArticle(anyLong(),anyLong())).willThrow(new InvalidUsageException(ExceptionStatus.FORBIDDEN,ExceptionType.AUTHOR_ONLY));

        mockMvc.perform(delete("/api/v1/article/1")
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("작성자만 이용할 수 있습니다."));
    }

    @DisplayName("게시글 검색 API")
    @MockUser
    @Test
    void searchArticle() throws Exception{
        List<ArticleDto> articleDtoList = new ArrayList<>();
        articleDtoList.add(ArticleDto.of(Article.of("게시글1", "인증1", INFO_QUESTION, FEEDBACK, PUBLIC, member), member, "testfile1.jpeg"));
        articleDtoList.add(ArticleDto.of(Article.of("게시글 운동", "인증2", INFO_QUESTION, FEEDBACK, PUBLIC, member), member, "testfile2.jpeg"));

        AllArticleResponse articleResponse = new AllArticleResponse(2,false,articleDtoList);

        given(elsArticleService.searchArticleByTitle(eq(memberId),eq("게시글"),eq(FEEDBACK),eq(2),isNull())).willReturn(articleResponse);

        mockMvc.perform(get("/api/v1/article/search")
                        .param("title","게시글")
                        .param("category","FEEDBACK")
                        .param("size","2")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.articles.size()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.articles.[1].title").value("게시글 운동"));
    }


    @DisplayName("게시글의 검색한 최근 검색어 조회")
    @MockUser
    @Test
    void searchRecentWords() throws Exception{
        List<Object> searchWords = List.of("기록1","기록2","기록3","기록4");

        given(redisService.getRecentSearchLogs(eq(memberId.toString()))).willReturn(searchWords);

        mockMvc.perform(get("/api/v1/article/search-record"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size()").value(4))
                .andExpect(jsonPath("$.data.[2]").value("기록3"));
    }
    
    @DisplayName("최근 게시글 내역 등록")
    @MockUser
    @Test
    void searchRecentSave() throws Exception {

        doNothing().when(redisService).setRecentSearchLog(eq(memberId.toString()),eq("제목1"));

        Map<String,String> request = Map.of("title","제목1");
        mockMvc.perform(post("/api/v1/article/search-record")
                        .content(ob.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("등록 성공"));
    }
    
    @DisplayName("최근 검색어 삭제 - 단일 삭제")
    @MockUser
    @Test
    void RecentSearchDelete() throws Exception {

        doNothing().when(redisService).deleteRecentSearchLog(eq(memberId.toString()),eq("제목"),isNull());
        
        mockMvc.perform(delete("/api/v1/article/search-record")
                .param("title","제목")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("삭제 성공"));
    }

    @DisplayName("최근 검색어 삭제 - 전체 삭제")
    @MockUser
    @Test
    void RecentSearchDeleteAll() throws Exception {

        doNothing().when(redisService).deleteRecentSearchLog(eq(memberId.toString()),isNull(),eq("all"));

        mockMvc.perform(delete("/api/v1/article/search-record")
                        .param("type","all")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("삭제 성공"));
    }
    
    

}