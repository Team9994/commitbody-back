package team9499.commitbody.domain.article.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateByQueryRequest;
import co.elastic.clients.elasticsearch.core.UpdateByQueryResponse;
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
import team9499.commitbody.global.utils.TimeConverter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    private final String ARTICLE_INDEX  = "article_index";

    @Async
    @Override
    public void saveArticle(ArticleDto articleDto) {
        ArticleDoc articleDoc = ArticleDoc.of(articleDto);
        elsArticleRepository.save(articleDoc);
    }

    @Override
    public AllArticleResponse searchArticleByTitle(Long memberId, String title,ArticleCategory category, Integer size, Long lastId) {
        List<Long> blockedIds = elsBlockMemberService.findBlockedIds(memberId);
        List<Long> blockerIds = elsBlockMemberService.getBlockerIds(memberId);

        // 차단된 사용자 차단한 사용자 ID 합치기
        blockedIds.addAll(blockerIds);

        BoolQuery.Builder builder = new BoolQuery.Builder();

        // 게시글 검색시 사용 되는 동적 로직
        if (title!=null){
            QueryStringQuery titleQuery = new QueryStringQuery.Builder()
                    .query("*" + title + "*")
                    .fields("title")
                    .defaultOperator(Operator.And).build();
            builder.must(Query.of(q -> q.queryString(titleQuery)));
        }
        
        // 카테고리를 필터링하는 동적 로직
        if (category!=null){
            TermQuery categoryTerm = new TermQuery.Builder()
                    .field("category")
                    .value(category.toString()).build();
            builder.must(Query.of(q -> q.term(categoryTerm)));
        }

        TermsQueryField termsQueryField = new TermsQueryField.Builder().value(blockedIds.stream().map(FieldValue::of).toList()).build();

        //차단 사용자의 게시물을 제외
        builder.mustNot(Query.of(q -> q.terms(t -> t.field("member_id").terms(termsQueryField))));
        
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(ARTICLE_INDEX)
                .query(Query.of(q -> q.bool(builder.build())))
                .size(size+1)
                .sort(SortOptions.of(s -> s.field(f -> f.field("id.keyword").order(SortOrder.Desc))))
                .searchAfter(builder1 -> builder1.stringValue(String.valueOf(lastId)))
                .trackTotalHits(tth -> tth.enabled(true))  // track_total_hits 하여 조회하는 전체 데이터 수 조회
                .build();


        try {
            SearchResponse<Object> response = elasticsearchClient.search(searchRequest, Object.class);
            long totalCount = response.hits().total().value();

            List<Hit<Object>> hits = new ArrayList<>(response.hits().hits());

            boolean hasNext = false;
            if (hits.size() > size) {
                hits.remove(hits.size()-1);
                hasNext = true;     // 다음 페이지가 있음을 나타내는 플래그 설정
            }

            List<ArticleDto> articleDtoList = new ArrayList<>();
            for (Hit<Object> hit : hits) {
                Map<String,Object> source = (Map<String, Object>) hit.source();
                ArticleDto articleDto = ArticleDto.of(
                        convertToLong(source.get("id")),                    // Long으로 변환
                        convertToLong(source.get("member_id")),
                        ArticleCategory.stringToEnum((String) source.get("category")),
                        (String) source.get("content"),
                        (String)source.get("title"),
                        (Integer) source.get("like_count"),
                        (Integer)source.get("comment_count"),
                        convertTime(source.get("time")),
                        (String) source.get("img_url"),
                        (String)source.get("writer"),
                        null);
                articleDtoList.add(articleDto);
            }

            return new AllArticleResponse((int) totalCount,hasNext,articleDtoList);
        } catch (Exception e) {
            log.error("에러", e);
            e.printStackTrace();
        }
        return null;
    }


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