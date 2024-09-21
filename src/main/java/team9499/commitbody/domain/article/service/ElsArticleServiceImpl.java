package team9499.commitbody.domain.article.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
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
import team9499.commitbody.domain.follow.repository.FollowRepository;
import team9499.commitbody.global.utils.TimeConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ElsArticleServiceImpl implements ElsArticleService{

    private final ElsArticleRepository elsArticleRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final ElsBlockMemberService elsBlockMemberService;
    private final FollowRepository followRepository;

    private final String ARTICLE_INDEX  = "article_index";

    /**
     * 비동기를 통한 엘라스틱 게시글 저장
     * @param articleDto 게시글DTO
     */
    @Async
    @Override
    public void saveArticleAsync(ArticleDto articleDto) {
        ArticleDoc articleDoc = ArticleDoc.of(articleDto);
        elsArticleRepository.save(articleDoc);
    }

    /**
     * 비동기를 통한 엘라스틱 게시글명, 카테고리,이미지주소를 변경
     * @param articleDto 게시글DTO
     */
    @Async
    @Override
    public void updateArticleAsync(ArticleDto articleDto) {

        Map<String,String> doc = new HashMap<>();
        doc.put("category",articleDto.getArticleCategory().toString());
        doc.put("title",articleDto.getTitle());
        doc.put("img_url",articleDto.getImageUrl());

        try {
            UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u -> u.index(ARTICLE_INDEX).id(String.valueOf(articleDto.getArticleId())).doc(doc));
            UpdateResponse<Object> updateResponse = elasticsearchClient.update(updateRequest, Map.class);
            log.info("게시글 수정 성공 ID ={}",updateResponse.id());
        }catch (Exception e){
            log.error("업데이트중 오류 발생");
        }
    }

    /**
     * 비동기를 통한 게시글 ID를 통한 게시글 삭제
     * @param articleId 삭제할 게시글 ID
     */
    @Async
    @Override
    public void deleteArticleAsync(Long articleId) {
        DeleteRequest deleteRequest = DeleteRequest.of(d -> d.index(ARTICLE_INDEX).id(String.valueOf(articleId)));
        try{
            DeleteResponse deleteResponse = elasticsearchClient.delete(deleteRequest);
            log.info("게시글 삭제 성공 ID ={} ",deleteResponse.id());
        }catch (Exception e){
            log.error("엘라스틱 게시글 삭제시 오류 발생");
        }
    }

    /**
     * 엘라스틱 게시글의 제목을 통한 검색 API
     * @param memberId 로그인한 사용자 ID
     * @param title 검색할 게시글 명
     * @param category  필터링한 카테고리
     * @param size  조회할 데이터 크기 기본 크기 10
     * @param lastId 마지막 조회 ID
     * @return  AllArticleResponse을 반환
     */

    @Override
    public AllArticleResponse searchArticleByTitle(Long memberId, String title,ArticleCategory category, Integer size, Long lastId) {
        List<Long> blockedIds = elsBlockMemberService.findBlockedIds(memberId);
        List<Long> blockerIds = elsBlockMemberService.getBlockerIds(memberId);
        List<Long> followings = followRepository.followings(memberId);
        // 차단된 사용자와 차단한 사용자 ID 합치기
        blockedIds.addAll(blockerIds);
        followings.add(memberId);   // 자신의 게시물을 확인하기 위해 추가
   
        BoolQuery.Builder builder = new BoolQuery.Builder();

        // 제목 필터링 (동적 조건)
        if (title != null) {
            QueryStringQuery titleQuery = new QueryStringQuery.Builder()
                    .query("*" + title + "*")
                    .fields("title")
                    .defaultOperator(Operator.And).build();
            builder.must(Query.of(q -> q.queryString(titleQuery)));
        }

        // 카테고리 필터링 (동적 조건)
        if (category != null) {
            TermQuery categoryTerm = new TermQuery.Builder()
                    .field("category")
                    .value(category.toString()).build();
            builder.must(Query.of(q -> q.term(categoryTerm)));
        }

        // 'PUBLIC' 게시글 조건 추가
        builder.should(Query.of(q -> q.term(t -> t.field("visibility").value("PUBLIC"))));

        // 'FOLLOWERS_ONLY' 게시글 조건 추가 (팔로우한 사용자들만)
        TermsQueryField followingQueryField = new TermsQueryField.Builder()
                .value(followings.stream().map(FieldValue::of).toList()).build();

        // 팔로워한 사용자들에게만 공개
        builder.should(Query.of(q -> q.bool(b -> b
                .must(Query.of(q2 -> q2.term(t -> t.field("visibility").value("FOLLOWERS_ONLY"))))
                .must(Query.of(q2 -> q2.terms(t -> t.field("member_id").terms(followingQueryField))))
        )));

        TermsQueryField termsQueryField = new TermsQueryField.Builder()
                .value(blockedIds.stream().map(FieldValue::of).toList()).build();

        builder.mustNot(Query.of(q -> q.terms(t -> t.field("member_id").terms(termsQueryField))),
                Query.of(b -> b.bool(bq -> bq
                        .must(m -> m.term(t -> t.field("visibility").value("PRIVATE")))
                        .mustNot(m -> m.term(t -> t.field("member_id").value(memberId))))));

        // Elasticsearch 검색 요청 빌드
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                .index(ARTICLE_INDEX)
                .query(Query.of(q -> q.bool(builder.build())))
                .size(size + 1)  // 한 페이지에 나오는 게시물 수
                .sort(SortOptions.of(s -> s.field(f -> f.field("id").order(SortOrder.Desc))))  // 내림차순 정렬
                .trackTotalHits(tth -> tth.enabled(true));  // 전체 데이터 수 조회



        if (lastId != null) {
            searchRequestBuilder.searchAfter(builder1 -> builder1.longValue(lastId));  // search_after로 페이징 처리
        }

        SearchRequest searchRequest = searchRequestBuilder.build();
        try {
            SearchResponse<Object> response = elasticsearchClient.search(searchRequest, Object.class);
            long totalCount = response.hits().total().value();

            List<Hit<Object>> hits = new ArrayList<>(response.hits().hits());

            boolean hasNext = false;
            if (hits.size() > size) {
                hits.remove(hits.size() - 1);
                hasNext = true;  // 다음 페이지가 있음을 나타내는 플래그 설정
            }

            List<ArticleDto> articleDtoList = new ArrayList<>();
            for (Hit<Object> hit : hits) {
                Map<String, Object> source = (Map<String, Object>) hit.source();
                ArticleDto articleDto = ArticleDto.of(
                        convertToLong(source.get("id")),
                        convertToLong(source.get("member_id")),
                        ArticleCategory.stringToEnum((String) source.get("category")),
                        (String) source.get("content"),
                        (String) source.get("title"),
                        (Integer) source.get("like_count"),
                        (Integer) source.get("comment_count"),
                        convertTime(source.get("time")),
                        (String) source.get("img_url"),
                        (String) source.get("writer"),
                        null);
                articleDtoList.add(articleDto);
            }

            return new AllArticleResponse((int) totalCount, hasNext, articleDtoList);
        } catch (Exception e) {
            log.error("에러", e);
        }
        return null;
    }


    /**
     * 사용자 프로필 변경시 게시글의 작성된 작성자 명을 비동기로 변경
     * @param beforeNickname    이전 사용자 닉네임
     * @param afterNickname     변경한 사용자 닉네임
     */
    @Async
    @Override
    public void updateWriterAsync(String beforeNickname, String afterNickname) {
        UpdateByQueryRequest request = new UpdateByQueryRequest.Builder()
                .index(ARTICLE_INDEX)
                .query(q -> q.match(m -> m
                                .field("writer")
                                .query(beforeNickname)))
                .script(s -> s.inline(i -> i
                                .source("ctx._source.writer = params.newWriter")
                                .lang("painless")
                                .params("newWriter", JsonData.of(afterNickname))))
                .build();

        try {
            UpdateByQueryResponse updateByQueryResponse = elasticsearchClient.updateByQuery(request);
            log.info("게시글 작성자 닉네임 업데이트 성공,  변경한 게시글 수 = {}",updateByQueryResponse.updated());
        } catch (Exception e) {
            log.error("업데이트 도중 에러 발생 = {}",e.getMessage());
        }
    }

    /**
     * 게시글 인덱스의 댓글 수와 좋아요수를 MySQL에 저장된 데이터와 일관성을 유지하기 위해 댓글,좋아요(삭제,추가)기능 동작시 업데이트 하는 로직을 실행
     * @param articleId 게시글 ID
     * @param count 변경된 수
     * @param type  댓글, 좋아요
     */
    @Async
    @Override
    public void updateArticleCountAsync(Long articleId, Integer count, String type) {
        Map<String,Integer> doc = new HashMap<>();

        switch (type){
            case "댓글" -> doc.put("comment_count",count);
        }
        
        try {
            UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u -> u.index(ARTICLE_INDEX).id(String.valueOf(articleId)).doc(doc));
            elasticsearchClient.update(updateRequest,Map.class);
        }catch (Exception e){
            log.error("게시글 댓글 수 업데이트중 오류 발생");
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
