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

import static team9499.commitbody.global.constants.Delimiter.*;
import static team9499.commitbody.global.constants.ElasticFiled.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ElasticExerciseInterestServiceImpl implements ElasticExerciseInterestService {

    private final ElasticsearchClient client;

    @Override
    public SearchExerciseResponse searchFavoriteExercise(String memberId, BoolQuery.Builder builder,int size, int from,List<SortOptions> sortOptions) {
        BoolQuery favoriteQuery = createFavoriteQuery(memberId);
        SearchRequest favoriteRequest = createSearchRequest(favoriteQuery);

        return processSearchExercise(builder, size, from, sortOptions, favoriteRequest);
    }

    @Override
    public List<ExerciseDto> updateInterestFieldStatus(String memberId, List<ExerciseDto> response) throws IOException {
        BoolQuery interestBool = getUpdateQuery(memberId, response);
        SearchRequest interestRequest = createInterestRequest(interestBool);
        SearchResponse<Object> interestResponse = client.search(interestRequest, Object.class);
        List<Hit<Object>> interestHits = interestResponse.hits().hits();

        Set<Long> exerciseIds = getInterestExerciseIds(interestHits);

        setInterestForMatchingExercises(response, exerciseIds);

        return response;
    }

    private List<ExerciseDto> getExerciseDtos(SearchResponse<Object> searchResponse) {
        return searchResponse.hits().hits().stream()
                .map(this::getExerciseDto)
                .toList();
    }

    private ExerciseDto getExerciseDto(Hit<Object> hit) {
        Map<String, Object> source = (Map<String, Object>) hit.source();
        Long exerciseId = Long.valueOf(source.get(EXERCISE_ID).toString());
        String name = (String) source.get(EXERCISE_NAME);
        String gifUrl = Optional.ofNullable(source.get(EXERCISE_GIF)).map(Object::toString).orElse(NO_IMAGE);
        String target = (String) source.get(EXERCISE_TARGET);
        String equipment = (String) source.get(EXERCISE_EQUIPMENT);
        String src = (String) source.get(SOURCE);
        return ExerciseDto.of(exerciseId, name, gifUrl, target, WEIGHT_AND_LEP, equipment, src, true);
    }

    private static BoolQuery createFavoriteQuery(String memberId) {
        return new BoolQuery.Builder()
                .must(m -> m.term(t -> t.field(MEMBER_ID).value(memberId)))
                .must(m -> m.term(t -> t.field(STATUS).value(true))).build();
    }

    private static SearchRequest createSearchRequest(BoolQuery favoriteQuery) {
        return new SearchRequest.Builder()
                .index(INTEREST_INDEX)
                .query(q -> q.bool(favoriteQuery))
                .size(10000)
                .build();
    }


    private static void searchBuilder(BoolQuery.Builder builder, List<Hit<Object>> hits) {
        List<String> exerciseIds = getExerciseIds(hits);

        TermsQueryField exerciseIdTerms = new TermsQueryField.Builder()
                .value(exerciseIds.stream().map(FieldValue::of).toList())
                .build();

        BoolQuery favoriteFilterQuery = new BoolQuery.Builder()
                .must(m -> m.terms(t -> t.field(ID).terms(exerciseIdTerms))).build();

        builder.must(Query.of(q -> q.bool(favoriteFilterQuery)));
    }

    private static List<String> getExerciseIds(List<Hit<Object>> hits) {
        List<String> exerciseIds = new ArrayList<>();
        for (Hit<Object> hit : hits) {
            Map<String, Object> source = (Map<String, Object>) hit.source();
            String id = source.get(ID).toString();
            exerciseIds.add(id.substring(0, id.indexOf(DASH)));
        }
        return exerciseIds;
    }

    private static SearchRequest getSearchRequest(BoolQuery.Builder builder, int size, int from, List<SortOptions> sortOptions) {
        return new SearchRequest.Builder()
                .index(EXERCISE_INDEX)
                .query(Query.of(q -> q.bool(builder.minimumShouldMatch(String.valueOf(1)).build())))
                .size(size)
                .from(from)
                .sort(sortOptions)
                .build();
    }

    private static long getValue(SearchResponse<Object> searchResponse) {
        return searchResponse.hits().total() != null ? searchResponse.hits().total().value() : 0;
    }

    private SearchExerciseResponse processSearchExercise(BoolQuery.Builder builder, int size, int from, List<SortOptions> sortOptions, SearchRequest favoriteRequest) {
        try {
            List<Hit<Object>> hits = favoriteResponseHit(favoriteRequest);

            searchBuilder(builder, hits);
            SearchResponse<Object> searchResponse = getObjectSearchResponse(builder, size, from, sortOptions);

            return new SearchExerciseResponse(getValue(searchResponse), getExerciseDtos(searchResponse));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private List<Hit<Object>> favoriteResponseHit(SearchRequest favoriteRequest) throws IOException {
        SearchResponse<Object> favoriteResponse = client.search(favoriteRequest, Object.class);
        return favoriteResponse.hits().hits();
    }

    private SearchResponse<Object> getObjectSearchResponse(BoolQuery.Builder builder, int size, int from, List<SortOptions> sortOptions) throws IOException {
        SearchRequest searchRequest = getSearchRequest(builder, size, from, sortOptions);
        return client.search(searchRequest, Object.class);
    }

    private static BoolQuery getUpdateQuery(String memberId, List<ExerciseDto> response) {
        TermsQueryField countryTerms = getUpdateTermsQuery(response);

        return new BoolQuery.Builder()
                .must(m -> m.term(t -> t.field(MEMBER_ID).value(memberId)))
                .must(m -> m.terms(t -> t.field(EXERCISE_ID).terms(countryTerms))).build();
    }

    private static TermsQueryField getUpdateTermsQuery(List<ExerciseDto> response) {
        List<Long> ids = response.stream()
                .map(ExerciseDto::getExerciseId)
                .toList();

        return new TermsQueryField.Builder()
                .value(ids.stream().map(FieldValue::of).toList())
                .build();
    }


    private static SearchRequest createInterestRequest(BoolQuery interestBool) {
        return new SearchRequest.Builder()
                .index(INTEREST_INDEX)
                .query(q -> q.bool(interestBool))
                .build();
    }

    private static Set<Long> getInterestExerciseIds(List<Hit<Object>> interestHits) {
        // 아이디값 추출
        return interestHits.stream()
                .filter(hit -> {
                    Map<String, Object> source = (Map<String, Object>) hit.source();
                    Boolean status = (Boolean) source.get(STATUS);
                    return Boolean.TRUE.equals(status);
                })
                .map(hit -> {
                    Map<String, Object> source = (Map<String, Object>) hit.source();
                    return Long.parseLong(source.get(EXERCISE_ID).toString()); // 아이디값 추출
                })
                .collect(Collectors.toSet());
    }

    private static void setInterestForMatchingExercises(List<ExerciseDto> response, Set<Long> exerciseIds) {
        response.forEach(exerciseDto -> {
            if (exerciseIds.contains(exerciseDto.getExerciseId())) {
                exerciseDto.setInterest(true);
            }
        });
    }

}
