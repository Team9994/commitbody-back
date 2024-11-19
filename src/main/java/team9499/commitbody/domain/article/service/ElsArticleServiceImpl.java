package team9499.commitbody.domain.article.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;

import java.util.Arrays;

import co.elastic.clients.util.ObjectBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.article.domain.ArticleCategory;
import team9499.commitbody.domain.article.domain.ArticleDoc;
import team9499.commitbody.domain.article.dto.ArticleDto;
import team9499.commitbody.domain.article.dto.response.AllArticleResponse;
import team9499.commitbody.domain.article.repository.ElsArticleRepository;
import team9499.commitbody.domain.block.servcice.ElsBlockMemberService;
import team9499.commitbody.domain.comment.article.service.ArticleCommentService;
import team9499.commitbody.domain.follow.repository.FollowRepository;
import team9499.commitbody.domain.like.service.LikeService;
import team9499.commitbody.global.utils.TimeConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static team9499.commitbody.domain.article.domain.ArticleCategory.*;
import static team9499.commitbody.domain.article.domain.Visibility.*;
import static team9499.commitbody.domain.article.domain.Visibility.PUBLIC;
import static team9499.commitbody.global.Exception.ExceptionStatus.*;
import static team9499.commitbody.global.constants.Delimiter.*;
import static team9499.commitbody.global.constants.ElasticFiled.*;

@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class ElsArticleServiceImpl implements ElsArticleService {

    private static final String MEMBER_iD = "member_id";
    private static final String NEW_WRITER = "newWriter";
    private static final String LIKE = "좋아요";
    private static final String COMMENT = "댓글";
    private static final String COUNT = "count";

    private final ElsArticleRepository elsArticleRepository;
    private final ElasticsearchClient client;
    private final ElsBlockMemberService elsBlockMemberService;
    private final LikeService likeService;
    private final ArticleCommentService articleCommentService;
    private final FollowRepository followRepository;

    /**
     * 비동기를 통한 엘라스틱 게시글 저장
     *
     * @param articleDto 게시글DTO
     */
    @Async
    @Override
    public void saveArticleAsync(ArticleDto articleDto) {
        elsArticleRepository.save(ArticleDoc.of(articleDto));
    }

    /**
     * 비동기를 통한 엘라스틱 게시글명, 카테고리,이미지주소를 변경
     *
     * @param articleDto 게시글DTO
     */
    @Async
    @Override
    public void updateArticleAsync(ArticleDto articleDto) {
        handleElasticUpdateArticle(articleDto, createUpdateDoc(articleDto));
    }

    /**
     * 비동기를 통한 게시글 ID를 통한 게시글 삭제
     *
     * @param articleId 삭제할 게시글 ID
     */
    @Async
    @Override
    public void deleteArticleAsync(Long articleId) {
        handleElasticDeleteArticle(articleId);
    }

    /**
     * 엘라스틱 게시글의 제목을 통한 검색 API
     *
     * @param memberId 로그인한 사용자 ID
     * @param title    검색할 게시글 명
     * @param category 필터링한 카테고리
     * @param size     조회할 데이터 크기 기본 크기 10
     * @param lastId   마지막 조회 ID
     * @return AllArticleResponse을 반환
     */

    @Override
    public AllArticleResponse searchArticleByTitle(Long memberId, String title, ArticleCategory category, Integer size, Long lastId) {
        BoolQuery.Builder builder = createSearchQueryBuilder(memberId, title, category);
        SearchRequest searchRequest = SearchRequest(size, lastId, builder);
        return handleSearchArticle(size, searchRequest);
    }

    /**
     * 사용자 프로필 변경시 게시글의 작성된 작성자 명을 비동기로 변경
     *
     * @param beforeNickname 이전 사용자 닉네임
     * @param afterNickname  변경한 사용자 닉네임
     */
    @Async
    @Override
    public void updateWriterAsync(String beforeNickname, String afterNickname) {
        UpdateByQueryRequest request = createUpdateRequest(beforeNickname, afterNickname);
        handleUpdateWriter(request);
    }

    /**
     * 게시글 인덱스의 댓글 수와 좋아요수를 MySQL에 저장된 데이터와 일관성을 유지하기 위해 댓글,좋아요(삭제,추가)기능 동작시 업데이트 하는 로직을 실행
     *
     * @param articleId 게시글 ID
     * @param count     변경된 수
     * @param type      댓글, 좋아요
     */
    @Async
    @Override
    public void updateArticleCountAsync(Long articleId, Integer count, String type) {
        UpdateRequest<Object, Object> updateRequest = updateArticleRequest(articleId, count, type);
        handleUpdateArticleCount(updateRequest);
    }


    @Async
    @Override
    public void updateArticleWithDrawAsync(Long memberId, Boolean type) {
        UpdateByQueryRequest queryRequest = createArticleWithDrawRequest(memberId, type);
        handleArticleWithDraw(queryRequest);
    }

    /**
     * 사용자 탈퇴및 재가입시 발생
     * - 탈퇴 및 재가입의 대한 사용자가 작성한 게시글의 대한 좋아요, 댓글의대한 카운트수를 감소및 증가
     *
     * @param memberId 사용자 ID
     * @param type     true : 탈퇴 , false : 재가입
     */
    @Override
    public void updateArticleLikeAndCommentCountAsync(Long memberId, Boolean type) {
        List<Long> writeDrawArticleIds = likeService.getWriteDrawArticleIds(memberId);
        List<Long> writeDrawArticleIdsByComment = articleCommentService.getWriteDrawArticleIdsByComment(memberId);
        BulkRequest.Builder bulkBuilder = getBulkBuilder(type, writeDrawArticleIdsByComment);
        UpdateByQueryRequest likeQueryRequest = createLikeQueryRequest(type, getLikeTermsQuery(writeDrawArticleIds));
        handleArticleLikeAndCommentCount(likeQueryRequest, bulkBuilder);
    }

    private static Map<String, String> createUpdateDoc(ArticleDto articleDto) {
        Map<String, String> doc = new HashMap<>();
        doc.put(CATEGORY, articleDto.getArticleCategory().toString());
        doc.put(TITLE, articleDto.getTitle());
        doc.put(IMG_URL, articleDto.getImageUrl());
        return doc;
    }

    private void handleElasticUpdateArticle(ArticleDto articleDto, Map<String, String> doc) {
        try {
            UpdateResponse<Object> updateResponse = client.update(createUpdateArticleRequest(articleDto, doc), Object.class);
            log.info("게시글 수정 성공 ID ={}", updateResponse.id());
        } catch (Exception e) {
            log.error("게시글 수정시 에러 발생");
        }
    }

    private static UpdateRequest<Object, Object> createUpdateArticleRequest(ArticleDto articleDto, Map<String, String> doc) {
        return UpdateRequest.of(u -> u.index(ARTICLE_INDEX).id(String.valueOf(articleDto.getArticleId())).doc(doc));
    }


    private void handleElasticDeleteArticle(Long articleId) {
        try {
            DeleteResponse deleteResponse = client.delete(createDeleteRequest(articleId));
            log.info("게시글 삭제 성공 ID ={} ", deleteResponse.id());
        } catch (Exception e) {
           log.error("게시글 삭제도중 에러 발생");
        }
    }

    private static DeleteRequest createDeleteRequest(Long articleId) {
        return DeleteRequest.of(d -> d.index(ARTICLE_INDEX).id(String.valueOf(articleId)));
    }

    private AllArticleResponse handleSearchArticle(Integer size, SearchRequest searchRequest) {
        try {
            SearchResponse<Object> response = client.search(searchRequest, Object.class);
            long totalCount = response.hits().total() != null ? response.hits().total().value() : 0;

            List<Hit<Object>> hits = new ArrayList<>(response.hits().hits());
            return new AllArticleResponse((int) totalCount, isHasNext(size, hits), getArticleDtoList(hits));
        } catch (Exception e) {
            log.error("검색도중 오류 발생");
        }
        return new AllArticleResponse();
    }

    private static boolean isHasNext(Integer size, List<Hit<Object>> hits) {
        boolean hasNext = false;
        if (hits.size() > size) {
            hits.remove(hits.size() - 1);
            hasNext = true;  // 다음 페이지가 있음을 나타내는 플래그 설정
        }
        return hasNext;
    }

    private List<ArticleDto> getArticleDtoList(List<Hit<Object>> hits) {
        List<ArticleDto> articleDtoList = new ArrayList<>();
        for (Hit<Object> hit : hits) {
            Map<String, Object> source = (Map<String, Object>) hit.source();
            ArticleDto articleDto = ArticleDto.of(
                    convertToLong(source.get(ID)),
                    convertToLong(source.get(MEMBER_ID)),
                    stringToEnum((String) source.get(CATEGORY)),
                    (String) source.get(CONTENT),
                    (String) source.get(TITLE),
                    (Integer) source.get(LIKE_COUNT),
                    (Integer) source.get(COMMENT_COUNT),
                    convertTime(source.get(TIME)),
                    (String) source.get(IMG_URL),
                    (String) source.get(WRITER),
                    null);
            articleDtoList.add(articleDto);
        }
        return articleDtoList;
    }


    private BoolQuery.Builder createSearchQueryBuilder(Long memberId, String title, ArticleCategory category) {
        List<Long> blockedIds = new ArrayList<>(elsBlockMemberService.findBlockedIds(memberId));
        List<Long> blockerIds = elsBlockMemberService.getBlockerIds(memberId);
        List<Long> followings = new ArrayList<>(followRepository.followings(memberId));
        // 차단된 사용자와 차단한 사용자 ID 합치기
        blockedIds.addAll(blockerIds);
        followings.add(memberId);   // 자신의 게시물을 확인하기 위해 추가

        return buildSearchQuery(memberId, title, category, followings, blockedIds);
    }

    private static BoolQuery.Builder buildSearchQuery(Long memberId, String title, ArticleCategory category, List<Long> followings, List<Long> blockedIds) {
        BoolQuery.Builder builder = new BoolQuery.Builder();
        titleBuilder(title, builder);
        categoryBuilder(category, builder);
        writDrawBuilder(builder);
        publicBuilder(builder);
        followerBuilder(followings, builder);
        privateBuilder(memberId, blockedIds, builder);
        return builder;
    }

    // 제목 필터링 (동적 조건)
    private static void titleBuilder(String title, BoolQuery.Builder builder) {
        if (title != null) {
            QueryStringQuery titleQuery = new QueryStringQuery.Builder()
                    .query(STAR + title + STAR)
                    .fields(TITLE)
                    .defaultOperator(Operator.And).build();
            builder.must(Query.of(q -> q.queryString(titleQuery)));
        }
    }

    private static void categoryBuilder(ArticleCategory category, BoolQuery.Builder builder) {
        if (category != null) {
            if (validEqCategoryName(category, builder)) return;
            builder.must(Query.of(q -> q.term(getCategoryTerm(category))));
        }
    }

    private static boolean validEqCategoryName(ArticleCategory category, BoolQuery.Builder builder) {
        if (category.name().equals(ALL.toString())) {
            List<String> categories = Arrays.asList(INFORMATION.toString(), FEEDBACK.toString(), BODY_REVIEW.toString());
            builder.must(m -> m.terms(t -> t.field(CATEGORY).terms(getTermsQueryField(categories))));
            return true;
        }
        return false;
    }

    private static TermsQueryField getTermsQueryField(List<String> categories) {
        return new TermsQueryField.Builder()
                .value(categories.stream().map(FieldValue::of).toList())
                .build();
    }

    private static TermQuery getCategoryTerm(ArticleCategory category) {
        return new TermQuery.Builder()
                .field(CATEGORY)
                .value(category.toString()).build();
    }

    private static void writDrawBuilder(BoolQuery.Builder builder) {
        // 탈퇴하지 않은 사용자의 게시글만 조회
        builder.must(Query.of(q -> q.term(t -> t.field(WRIT_DRAW).value(false))));
    }

    // 'PUBLIC' 게시글 조건 추가
    private static void publicBuilder(BoolQuery.Builder builder) {
        builder.should(Query.of(q -> q.term(t -> t.field(VISIBILITY).value(PUBLIC.toString()))));
    }

    private static void followerBuilder(List<Long> followings, BoolQuery.Builder builder) {
        // 'FOLLOWERS_ONLY' 게시글 조건 추가 (팔로우한 사용자들만)
        TermsQueryField followingQueryField = new TermsQueryField.Builder()
                .value(followings.stream().map(FieldValue::of).toList()).build();

        // 팔로워한 사용자들에게만 공개
        builder.should(Query.of(q -> q.bool(b -> b
                .must(Query.of(q2 -> q2.term(t -> t.field(VISIBILITY).value(FOLLOWERS_ONLY.toString()))))
                .must(Query.of(q2 -> q2.terms(t -> t.field(MEMBER_iD).terms(followingQueryField))))
        )));
    }


    private static void privateBuilder(Long memberId, List<Long> blockedIds, BoolQuery.Builder builder) {
        TermsQueryField termsQueryField = new TermsQueryField.Builder()
                .value(blockedIds.stream().map(FieldValue::of).toList()).build();

        builder.mustNot(Query.of(q -> q.terms(t -> t.field(MEMBER_iD).terms(termsQueryField))),
                Query.of(b -> b.bool(bq -> bq
                        .must(m -> m.term(t -> t.field(VISIBILITY).value(PRIVATE.toString())))
                        .mustNot(m -> m.term(t -> t.field(MEMBER_iD).value(memberId))))));
    }

    private static SearchRequest SearchRequest(Integer size, Long lastId, BoolQuery.Builder builder) {
        SearchRequest.Builder searchRequestBuilder = getSearchRequestBuilder(size, builder);
        if (lastId != null) {
            searchRequestBuilder.searchAfter(builder1 -> builder1.longValue(lastId));  // search_after로 페이징 처리
        }
        return searchRequestBuilder.build();
    }

    private static SearchRequest.Builder getSearchRequestBuilder(Integer size, BoolQuery.Builder builder) {
        return new SearchRequest.Builder()
                .index(ARTICLE_INDEX)
                .query(Query.of(q -> q.bool(builder.build())))
                .size(size + 1)  // 한 페이지에 나오는 게시물 수
                .sort(SortOptions.of(s -> s.field(f -> f.field(ID).order(SortOrder.Desc))))  // 내림차순 정렬
                .trackTotalHits(tth -> tth.enabled(true));
    }

    private static UpdateByQueryRequest createUpdateRequest(String beforeNickname, String afterNickname) {
        return new UpdateByQueryRequest.Builder()
                .index(ARTICLE_INDEX)
                .query(q -> q.match(m -> m
                        .field(WRITER)
                        .query(beforeNickname)))
                .script(s -> s.inline(i -> i
                        .source(CTX_WITH_DRAW)
                        .lang(PAINLESS)
                        .params(NEW_WRITER, JsonData.of(afterNickname))))
                .build();
    }

    private void handleUpdateWriter(UpdateByQueryRequest request) {
        try {
            UpdateByQueryResponse updateByQueryResponse = client.updateByQuery(request);
            log.info("게시글 작성자 닉네임 업데이트 성공,  변경한 게시글 수 = {}", updateByQueryResponse.updated());
        } catch (Exception e) {
          log.error("게시글 작성자 닉네임 업데이트 에러 발생");
        }
    }

    private static UpdateRequest<Object, Object> updateArticleRequest(Long articleId, Integer count, String type) {
        Map<String, Integer> doc = createArticleCountDoc(count, type);
        return UpdateRequest.of(u -> u.index(ARTICLE_INDEX).id(String.valueOf(articleId)).doc(doc));
    }

    private static Map<String, Integer> createArticleCountDoc(Integer count, String type) {
        Map<String, Integer> doc = new HashMap<>();
        switch (type) {
            case COMMENT -> doc.put(COMMENT_COUNT, count);
            case LIKE -> doc.put(LIKE_COUNT, count);
        }
        return doc;
    }

    private void handleUpdateArticleCount(UpdateRequest<Object, Object> updateRequest) {
        try {
            client.update(updateRequest, Object.class);
        } catch (Exception e) {
           log.error("게시글 카운트 업데이트 발생");
        }
    }

    private void handleArticleWithDraw(UpdateByQueryRequest queryRequest) {
        try {
            UpdateByQueryResponse updateByQueryResponse = client.updateByQuery(queryRequest);
            log.info("탈퇴한 사용자의 게시글 수 ={}", updateByQueryResponse.updated());
        } catch (Exception e) {
            log.error("탈퇴한 사용자 게시글수 수정시 에러 발생");
        }
    }

    private static UpdateByQueryRequest createArticleWithDrawRequest(Long memberId, Boolean type) {
        return new UpdateByQueryRequest.Builder()
                .index(ARTICLE_INDEX)
                .query(Query.of(q -> q.bool(b -> b.must(m -> m.term(t -> t.field(MEMBER_ID).value(memberId))))))
                .script(s -> s.inline(i -> i.source(CTX_WITH_DRAW)
                        .lang(PAINLESS)
                        .params(WRIT_DRAW, JsonData.of(type)))).build();
    }

    private static BulkRequest.Builder getBulkBuilder(Boolean type, List<Long> writeDrawArticleIdsByComment) {
        final String commentScript = type ? DECREMENT_COMMENT_COUNT : INCREMENT_COMMENT_COUNT;
        BulkRequest.Builder br = new BulkRequest.Builder();
        applyBulkOperationsForComments(writeDrawDoc(writeDrawArticleIdsByComment), br, commentScript);
        return br;
    }

    private static Map<Long, Integer> writeDrawDoc(List<Long> writeDrawArticleIdsByComment) {
        Map<Long, Integer> map = new HashMap<>();
        for (Long l : writeDrawArticleIdsByComment) {
            map.put(l, map.getOrDefault(l, 0) + 1);
        }
        return map;
    }

    private static void applyBulkOperationsForComments(Map<Long, Integer> map, BulkRequest.Builder br, String commentScript) {
        for (Map.Entry<Long, Integer> entry : map.entrySet()) {
            br.operations(op -> createUpdateOperationForComment(commentScript, entry, op));
        }
    }

    private static ObjectBuilder<BulkOperation> createUpdateOperationForComment(String commentScript, Map.Entry<Long, Integer> entry, BulkOperation.Builder op) {
        return op.update(u -> u
                .index(ARTICLE_INDEX)
                .id(entry.getKey().toString())
                .action(a -> a.script(s ->
                        s.inline(i -> i
                                .source(commentScript)
                                .lang(PAINLESS)
                                .params(COUNT, JsonData.of(entry.getValue()))
                        )
                )));
    }

    private static UpdateByQueryRequest createLikeQueryRequest(Boolean type, TermsQueryField likeTermsQuery) {
        final String likeScript = type ? DECREMENT_LIKE_COUNT : INCREMENT_LIKE_COUNT;
        return new UpdateByQueryRequest.Builder()
                .index(ARTICLE_INDEX)
                .query(Query.of(q -> q.terms(t -> t.field(_ID).terms(likeTermsQuery))))
                .script(s -> s.inline(i -> i.source(likeScript).lang(PAINLESS))).build();
    }

    private static TermsQueryField getLikeTermsQuery(List<Long> writeDrawArticleIds) {
        return new TermsQueryField.Builder().value(writeDrawArticleIds.stream().map(FieldValue::of).toList()).build();
    }

    private void handleArticleLikeAndCommentCount(UpdateByQueryRequest likeQueryRequest, BulkRequest.Builder bulkBuilder) {
        try {
            client.updateByQuery(likeQueryRequest);
            client.bulk(bulkBuilder.build());
        } catch (Exception e) {
            log.error("게시글 카운트 변경중 오류 발생 = {}", e.getMessage());
        }
    }


    /*
    저장된 사긴타입을 몇분전으로 변환하기 위한 컨버터
     */
    private String convertTime(Object obTime) {

        String time = (String) obTime;
        LocalDateTime localDateTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss"));
        return TimeConverter.converter(localDateTime);
    }

    private Long convertToLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value cannot be parsed as Long: " + value, e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported type for conversion: " + value.getClass());
        }
    }
}
