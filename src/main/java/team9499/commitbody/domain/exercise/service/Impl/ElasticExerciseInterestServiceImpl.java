package team9499.commitbody.domain.exercise.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.exercise.dto.SearchExerciseResponse;
import team9499.commitbody.domain.exercise.service.ElasticExerciseInterestService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static team9499.commitbody.global.utils.ElasticFiled.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ElasticExerciseInterestServiceImpl implements ElasticExerciseInterestService {

    private final ElasticsearchClient client;

    @Override
    public SearchExerciseResponse searchFavoriteExercise(String memberId, BoolQuery.Builder builder,int size, int from,List<SortOptions> sortOptions) {
        BoolQuery favoriteQuery = new BoolQuery.Builder()
                .must(m -> m.term(t -> t.field(MEMBER_FILED).value(memberId)))
                .must(m -> m.term(t -> t.field(STATUS).value(true))).build();

        SearchRequest favoriteRequest = new SearchRequest.Builder()
                .index(INTEREST_INDEX)
                .query(q -> q.bool(favoriteQuery))
                .size(10000)
                .build();

        try {
            SearchResponse<Object> favoriteResponse = client.search(favoriteRequest, Object.class);
            List<Hit<Object>> hits = favoriteResponse.hits().hits();

            List<String> exerciseIds = new ArrayList<>();
            for (Hit<Object> hit : hits) {
                Map<String, Object> source = (Map<String, Object>) hit.source();
                String id = source.get("id").toString();
                exerciseIds.add(id.substring(0, id.indexOf('-')));
            }

            TermsQueryField exerciseIdTerms = new TermsQueryField.Builder()
                    .value(exerciseIds.stream().map(FieldValue::of).toList())
                    .build();

            BoolQuery favoriteFilterQuery = new BoolQuery.Builder()
                    .must(m -> m.terms(t -> t.field("id").terms(exerciseIdTerms))).build();

            builder.must(Query.of(q -> q.bool(favoriteFilterQuery)));

            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(EXERCISE_INDEX)
                    .query(Query.of(q -> q.bool(builder.minimumShouldMatch(String.valueOf(1)).build())))
                    .size(size)
                    .from(from)
                    .sort(sortOptions)
                    .build();

            SearchResponse<Object> searchResponse = client.search(searchRequest, Object.class);

            long value = searchResponse.hits().total() != null ? searchResponse.hits().total().value() : 0;

            List<ExerciseDto> response = searchResponse.hits().hits().stream()
                    .map(hit -> {
                        Map<String, Object> source = (Map<String, Object>) hit.source();
                        Long exerciseId = Long.valueOf(source.get(EXERCISE_ID).toString());
                        String name = (String) source.get(EXERCISE_NAME);
                        String gifUrl = Optional.ofNullable(source.get(EXERCISE_GIF)).map(Object::toString).orElse("등록된 이미지 파일이 없습니다.");
                        String target = (String) source.get(EXERCISE_TARGET);
                        String type = "무게와 횟수"; // 고정값
                        String equipment = (String) source.get(EXERCISE_EQUIPMENT);
                        String src = (String) source.get(SOURCE);
                        return ExerciseDto.of(exerciseId, name, gifUrl, target, type, equipment, src, true);
                    })
                    .toList();

            return new SearchExerciseResponse(value,response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ExerciseDto> updateInterestFieldStatus(String memberId, List<ExerciseDto> response) throws IOException {

        List<Long> ids = response.stream()
                .map(ExerciseDto::getExerciseId)
                .toList();

        TermsQueryField countryTerms = new TermsQueryField.Builder()
                .value(ids.stream().map(FieldValue::of).toList())
                .build();

        BoolQuery interestBool = new BoolQuery.Builder()
                .must(m -> m.term(t -> t.field(MEMBER_FILED).value(memberId)))
                .must(m -> m.terms(t -> t.field(EXERCISE_ID).terms(countryTerms))).build();

        SearchRequest interestRequest = new SearchRequest.Builder()
                .index(INTEREST_INDEX)
                .query(q -> q.bool(interestBool))
                .build();

        SearchResponse<Object> interestResponse = client.search(interestRequest, Object.class);
        List<Hit<Object>> interestHits = interestResponse.hits().hits();

        Set<Long> exerciseIds = interestHits.stream()
                .filter(hit -> {
                    Map<String, Object> source = (Map<String, Object>) hit.source();
                    Boolean status = (Boolean) source.get("status");
                    return Boolean.TRUE.equals(status);
                })
                .map(hit -> {
                    Map<String, Object> source = (Map<String, Object>) hit.source();
                    return Long.parseLong(source.get(EXERCISE_ID).toString()); // 아이디값 추출
                })
                .collect(Collectors.toSet());

        response.forEach(exerciseDto -> {
            if (exerciseIds.contains(exerciseDto.getExerciseId())) {
                exerciseDto.setInterest(true);
            }
        });

        return response;
    }
}
