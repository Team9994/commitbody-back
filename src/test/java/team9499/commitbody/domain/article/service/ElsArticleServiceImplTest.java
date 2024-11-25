package team9499.commitbody.domain.article.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.article.domain.Article;
import team9499.commitbody.domain.article.domain.ArticleDoc;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.article.dto.response.AllArticleResponse;
import team9499.commitbody.domain.article.repository.ElsArticleRepository;
import team9499.commitbody.domain.block.servcice.impl.ElsBlockMemberServiceImpl;
import team9499.commitbody.domain.comment.article.service.ArticleCommentService;
import team9499.commitbody.domain.follow.repository.FollowRepository;
import team9499.commitbody.domain.like.service.impl.LikeServiceImpl;
import team9499.commitbody.mock.MockUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static team9499.commitbody.domain.article.domain.ArticleCategory.FEEDBACK;
import static team9499.commitbody.domain.article.domain.ArticleType.INFO_QUESTION;
import static team9499.commitbody.domain.article.domain.Visibility.PUBLIC;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ElsArticleServiceImplTest {

    @Mock private ElsArticleRepository elsArticleRepository;
    @Mock private ElasticsearchClient client;
    @Mock private ElsBlockMemberServiceImpl elsBlockMemberService;
    @Mock private LikeServiceImpl likeService;
    @Mock private ArticleCommentService articleCommentService;
    @Mock private FollowRepository followRepository;

    @InjectMocks
    private ElsArticleServiceImpl elsArticleService;

    private Long memberId = 1L;
    private Member member;
    private ArticleDto articleDto;

    @BeforeEach
    void init() {
        member = Member.builder().id(memberId).loginType(LoginType.KAKAO).nickname("테스트 닉네임").socialId("test").build();
        articleDto = ArticleDto.of(Article.of("게시글1", "인증1", INFO_QUESTION, FEEDBACK, PUBLIC, member), member, "http://testimage.com/img.jpeg");
        articleDto.setArticleId(1L);
    }

    @DisplayName("엘라스틱 게시글 저장 - 비동기 테스트")
    @MockUser
    @Test
    void elsArticleSaveAsync() throws Exception {
        ArticleDoc articleDoc = ArticleDoc.of(articleDto);
        when(elsArticleRepository.save(any(ArticleDoc.class))).thenReturn(articleDoc);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> elsArticleService.saveArticleAsync(articleDto));

        future.get();

        verify(elsArticleRepository, times(1)).save(any(ArticleDoc.class));
    }


    @DisplayName("엘라스틱 - 게시글 업데이트")
    @Test
    void updateArticle() throws Exception {
        UpdateResponse<Object> mockResponse = mock(UpdateResponse.class);
        when(mockResponse.id()).thenReturn("1");

        when(client.update(any(UpdateRequest.class), any())).thenReturn(mockResponse);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> elsArticleService.updateArticleAsync(articleDto));
        future.get();

        verify(client).update(any(UpdateRequest.class), any());
    }

    @DisplayName("엘라스틱 게시글 삭제")
    @Test
    void deleteArticleAsync() throws IOException {
        DeleteResponse mock = mock(DeleteResponse.class);
        when(mock.id()).thenReturn("1");

        when(client.delete(any(DeleteRequest.class))).thenReturn(mock);

        elsArticleService.deleteArticleAsync(1l);

        verify(client, times(1)).delete(any(DeleteRequest.class));


    }

    @DisplayName("닉네임 변경")
    @Test
    public void updateWriterAsync() throws Exception {
        UpdateByQueryResponse updateByQueryResponse = mock(UpdateByQueryResponse.class);
        when(updateByQueryResponse.updated()).thenReturn(1L); // 가상의 업데이트된 문서 수

        when(client.updateByQuery(any(UpdateByQueryRequest.class))).thenReturn(updateByQueryResponse);

        elsArticleService.updateWriterAsync("변경전", "변경후");

        verify(client, times(1)).updateByQuery(any(UpdateByQueryRequest.class));
    }

    @DisplayName("게시글 좋아요/댓글수 업데이트")
    @Test
    void updateCountArticle() throws IOException {
        Long articleId = 1L;
        Integer count = 5;
        String type = "댓글";

        UpdateResponse mockResponse = mock(UpdateResponse.class);

        Map<String, Integer> doc = new HashMap<>();
        doc.put("comment_count", count);

        doReturn(mockResponse).when(client).update((UpdateRequest<Object, Object>) any(), any());

        elsArticleService.updateArticleCountAsync(articleId, count, type);

        verify(client, times(1)).update(any(UpdateRequest.class), any());
    }

    @DisplayName("회원 탈퇴시 게시글 상태 업데이트")
    @Test
    void updateWithDraw() throws Exception {
        UpdateByQueryResponse updateByQueryResponse = mock(UpdateByQueryResponse.class);
        when(updateByQueryResponse.updated()).thenReturn(3l);

        when(client.updateByQuery(any(UpdateByQueryRequest.class))).thenReturn(updateByQueryResponse);

        elsArticleService.updateArticleWithDrawAsync(memberId, true);

        verify(client, times(1)).updateByQuery(any(UpdateByQueryRequest.class));
    }

    @DisplayName("게시글의 포함된 댓글/좋아요 수를 업데이트")
    @Test
    void updateCommentAndLikCount() throws Exception {
        List<Long> writeDrawArticleIds = List.of(1L, 2L, 3L, 4L);
        List<Long> writeDrawArticleIdsByComment = List.of(5L, 6L, 7L, 8L);
        UpdateByQueryResponse updateByQueryResponse = mock(UpdateByQueryResponse.class);
        BulkResponse bulkResponse = mock(BulkResponse.class);

        when(likeService.getWriteDrawArticleIds(eq(memberId))).thenReturn(writeDrawArticleIds);
        when(articleCommentService.getWriteDrawArticleIdsByComment(eq(memberId))).thenReturn(writeDrawArticleIdsByComment);



        when(client.updateByQuery(any(UpdateByQueryRequest.class))).thenReturn(updateByQueryResponse);
        when(client.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

        elsArticleService.updateArticleLikeAndCommentCountAsync(memberId,true);


        verify(client,times(1)).updateByQuery(any(UpdateByQueryRequest.class));
        verify(client,times(1)).bulk(any(BulkRequest.class));

    }
    
    @DisplayName("제목으로 게시글 검색")
    @Test
    void searchArticleByTitle() throws Exception{
        List<Long> blockedIds = List.of(1L,2L);
        List<Long> blockerIds = List.of(5L,6L);
        List<Long> followings = List.of();
        SearchResponse<Object> mockResponse = mock(SearchResponse.class);

        when(elsBlockMemberService.findBlockedIds(eq(memberId))).thenReturn(blockedIds);
        when(elsBlockMemberService.getBlockerIds(eq(memberId))).thenReturn(blockerIds);
        when(followRepository.followings(eq(memberId))).thenReturn(followings);

        List<Hit<Object>> mockHitList = new ArrayList<>();

        Map<String,Object> source1 = new HashMap<>();
        source1.put("id",1L);
        source1.put("memberId",1L);
        source1.put("category", "FEEDBACK");
        source1.put("content", "테스트 내용 1");
        source1.put("title", "테스트 제목 1");
        source1.put("like_count", 10);
        source1.put("comment_count", 5);
        source1.put("time", "2024-09-28T17:34:10");
        source1.put("img_url", "http://example.com/image.jpg");
        source1.put("writer", "Test Writer");

        Map<String,Object> source2 = new HashMap<>();
        source2.put("id",2L);
        source2.put("memberId",1L);
        source2.put("category", "FEEDBACK");
        source2.put("content", "테스트 내용 2");
        source2.put("title", "테스트 제목 2");
        source2.put("like_count", 140);
        source2.put("comment_count", 51);
        source2.put("time", "2024-09-28T17:34:10");
        source2.put("img_url", "http://example.com/image.jpg");
        source2.put("writer", "Test Writer");


        Hit mockHit1 = mock(Hit.class);
        Hit mockHit2 = mock(Hit.class);

        when(mockHit1.source()).thenReturn(source1);
        when(mockHit2.source()).thenReturn(source2);
        mockHitList.add(mockHit1);
        mockHitList.add(mockHit2);


        HitsMetadata hitsMetadata = mock(HitsMetadata.class);
        when(hitsMetadata.hits()).thenReturn(mockHitList);

        when(hitsMetadata.total()).thenReturn(new TotalHits.Builder().value(2L).relation(TotalHitsRelation.Eq).build());

        when(mockResponse.hits()).thenReturn(hitsMetadata);

        when(client.search(any(SearchRequest.class),any())).thenReturn(mockResponse);

        AllArticleResponse allArticleResponse = elsArticleService.searchArticleByTitle(memberId, "제목", FEEDBACK, 10, null);

        assertThat(allArticleResponse.getTotalCount()).isEqualTo(2);
        assertThat(allArticleResponse.getArticles().get(0).getTitle()).isEqualTo("테스트 제목 1");
        assertThat(allArticleResponse.isHasNext()).isFalse();
        assertThat(allArticleResponse.getArticles().get(0).getTime()).isInstanceOf(String.class);
    }
    
}