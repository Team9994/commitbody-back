package team9499.commitbody.domain.exercise.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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
import team9499.commitbody.domain.exercise.domain.ExerciseDoc;
import team9499.commitbody.domain.exercise.domain.ExerciseInterestDoc;
import team9499.commitbody.domain.exercise.dto.CustomExerciseDto;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.exercise.dto.SearchExerciseResponse;
import team9499.commitbody.domain.exercise.repository.ExerciseElsInterestRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseElsRepository;
import team9499.commitbody.domain.exercise.service.ElasticExerciseInterestService;
import team9499.commitbody.domain.exercise.service.ElasticExerciseService;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.ServerException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static team9499.commitbody.global.constants.Delimiter.*;
import static team9499.commitbody.global.constants.ElasticFiled.*;


@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class ElasticExerciseServiceImpl implements ElasticExerciseService {

    private final ExerciseElsInterestRepository exerciseElsInterestRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final ElasticExerciseInterestService exerciseFavoriteService;
    private final ExerciseElsRepository exerciseElsRepository;

    /**
     * 운동 검색을 위한 메서드
     * @param name  운동명
     * @param target    운동 부위
     * @param equipment 운동 장비
     * @param from      페이지
     * @param size      페이지 사이즈
     * @param memberId  로그인한 사용자 ID
     */
    @Override
    public SearchExerciseResponse searchExercise(String name, String target, String equipment, Integer from, Integer size, Boolean favorites, String memberId,String exerciseType) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        builder(name, target, equipment, memberId, exerciseType, boolQueryBuilder);
        List<SortOptions> options = getSortOptions();

        // 관심운동의 대한 요청 처리
        if (favorites!=null){
            return exerciseFavoriteService.searchFavoriteExercise(memberId,boolQueryBuilder,size,from,options);
        }
        SearchRequest searchRequest = getSearchRequest(from, size, boolQueryBuilder, options);

        return getExerciseResponse(memberId, searchRequest);
    }

    /**
     * 커스텀 운동 저장
     */
    @Async
    @Override
    public void saveExercise(CustomExerciseDto customExerciseDto) {
        ExerciseDoc exerciseDoc = ExerciseDoc.customExercise(customExerciseDto);
        exerciseElsRepository.save(exerciseDoc);
    }

    @Async
    @Override
    public void updateExercise(CustomExerciseDto customExerciseDto,String source) {
        Map<String, String> doc = createUpdateDoc(customExerciseDto);
        handleUpdateExercise(customExerciseDto, source, doc);
    }

    /**
     * 엘라스틱 커스텀 운동 삭제 메서드
     */
    @Async
    @Override
    public void deleteExercise(Long customExerciseId,Long memberId) {
        DeleteRequest deleteRequest = DeleteRequest.of(u -> u.index(EXERCISE_INDEX).id(CUSTOM_+customExerciseId+DASH+memberId));
        handleDeleteExercise(deleteRequest);
    }


    @Override
    public void changeInterest(Long exerciseId, String source,String status,Long memberId) {
        ExerciseInterestDoc exerciseInterestDoc = ExerciseInterestDoc.of(source + exerciseId+DASH+memberId,memberId, exerciseId, status.equals(ADD),false);
        exerciseElsInterestRepository.save(exerciseInterestDoc);
    }

    /**
     * 관심운동 도큐먼트에 탈퇴한 사용자의 withDraw 필드값을 false 업데이트 및
     * 재가입한 사용자의 값 또란 true 변경
     * @param memberId 현재 탈퇴한 사용자 Id
     * @param type 변경할 타입
     */
    @Async
    @Override
    public void updateExerciseInterestWithDrawAsync(Long memberId, boolean type) {
        UpdateByQueryRequest queryRequest = createUpdateInterestRequest(memberId, type);
        handleUpdateExerciseInterest(queryRequest);
    }

    private void builder(String name, String target, String equipment, String memberId, String exerciseType, BoolQuery.Builder boolQueryBuilder) {
        buildExerciseNameQuery(name, boolQueryBuilder);
        buildExerciseEquipmentQuery(equipment, boolQueryBuilder);
        buildTargetQuery(target, EXERCISE_TARGET, boolQueryBuilder);
        buildExerciseTypeQuery(exerciseType, boolQueryBuilder);
        addCurrentUserFilter(memberId, boolQueryBuilder); // 현재 사용자의 대해서만 조회
    }

    private void addCurrentUserFilter(String memberId, BoolQuery.Builder boolQueryBuilder) {
        boolQueryBuilder.should(Query.of(
                        q -> q.bool(
                                b -> b.mustNot(
                                        m ->m.exists(
                                                e -> e.field(MEMBER_ID)
                                        )
                                )
                        )))
                .should(Query.of(S -> S.term(t -> t.field(MEMBER_ID).value(memberId))));
    }

    private static void buildExerciseTypeQuery(String exerciseType, BoolQuery.Builder boolQueryBuilder) {
        if (exerciseType !=null && !exerciseType.isEmpty()){
            TermQuery termQuery = new TermQuery.Builder()
                    .field(SOURCE)
                    .value(exerciseType).build();
            boolQueryBuilder.filter(Query.of(q -> q.term(termQuery)));
        }
    }

    private void buildTargetQuery(String target, String TARGET_FIELD, BoolQuery.Builder boolQueryBuilder) {
        if (target != null && !target.isEmpty()) {
            TermQuery termQuery = new TermQuery.Builder()
                    .field(TARGET_FIELD)
                    .value(target).build();
            boolQueryBuilder.filter(Query.of(q -> q.term(termQuery)));
        }
    }

    private void buildExerciseEquipmentQuery(String equipment, BoolQuery.Builder boolQueryBuilder) {
        if (equipment !=null && !equipment.isEmpty()){
            TermQuery termQuery = new TermQuery.Builder()
                    .field(EXERCISE_EQUIPMENT)
                    .value(equipment).build();
            boolQueryBuilder.filter(Query.of(q-> q.term(termQuery)));
        }
    }

    private void buildExerciseNameQuery(String name, BoolQuery.Builder boolQueryBuilder) {
        if (name !=null&& !name.isEmpty()) {
            QueryStringQuery queryStringQuery = new QueryStringQuery.Builder()
                    .query(STAR + name + STAR)  // 와일드카드를 사용한 검색 쿼리  정확성이 조금 낮음
                    .fields(EXERCISE_NAME)
                    .defaultOperator(Operator.And).build();
            boolQueryBuilder.must(Query.of(q -> q.queryString(queryStringQuery)));
        }
    }

    private static List<SortOptions> getSortOptions() {
        return List.of(
                SortOptions.of(s -> s.field(f -> f.field(_SCORE).order(SortOrder.Asc))),
                SortOptions.of(s -> s.field(f -> f.field(_SOURCE).field(EXERCISE_ID).order(SortOrder.Asc))),
                SortOptions.of(s -> s.field(f -> f.field(_SCORE).order(SortOrder.Asc)))
        );
    }

    private SearchExerciseResponse getSearchExerciseResponse(String memberId, SearchRequest searchRequest) throws IOException {
        SearchResponse<Object> searchResponse = elasticsearchClient.search(searchRequest, Object.class);
        long value = searchResponse.hits().total() != null ? searchResponse.hits().total().value() : 0;     // 검색한 총 데이터 수

        List<ExerciseDto> response = searchResponse.hits().hits().stream()
                .map(this::createExerciseDtoWithHits).toList();
        response = exerciseFavoriteService.updateInterestFieldStatus(memberId, response);

        return new SearchExerciseResponse(value, response);
    }

    private static SearchRequest getSearchRequest(Integer from, Integer size, BoolQuery.Builder boolQueryBuilder, List<SortOptions> options) {
        return new SearchRequest.Builder()
                .index(EXERCISE_INDEX)
                .query(Query.of(q -> q.bool(boolQueryBuilder.minimumShouldMatch(String.valueOf(1)).build())))       // bool 최소 만족 조건 1 -> or
                .size(size)
                .from(from)
                .sort(options)
                .build();   // 사용자가 추가한 운동은 맨 아래로
    }
    private ExerciseDto createExerciseDtoWithHits(Hit<Object> hit) {
        Map<String, Object> source = (Map<String, Object>) hit.source();
        Long exerciseId = Long.valueOf(source.get(EXERCISE_ID).toString());
        String exName = (String) source.get(EXERCISE_NAME);
        String gifUrl = Optional.ofNullable(source.get(EXERCISE_GIF)).map(Object::toString).orElse(NO_IMAGE);
        String exTarget = (String) source.get(EXERCISE_TARGET);
        String type = source.get(EXERCISE_TYPE) !=null ? source.get(EXERCISE_TYPE).toString() : WEIGHT_AND_LEP;
        String exEquipment = (String) source.get(EXERCISE_EQUIPMENT);
        String src = (String) source.get(SOURCE);
        return ExerciseDto.of(exerciseId, exName, gifUrl, exTarget, type, exEquipment, src, false);
    }

    private SearchExerciseResponse getExerciseResponse(String memberId, SearchRequest searchRequest) {
        try {
            return getSearchExerciseResponse(memberId, searchRequest);
        }catch (Exception e){
            log.error("운동 검색중 오류 발생 ={}",e.getMessage());
            throw new ServerException(ExceptionStatus.INTERNAL_SERVER_ERROR, ExceptionType.SERVER_ERROR);
        }
    }

    private void elasticUpdateRequest(CustomExerciseDto customExerciseDto, String source, Map<String, String> doc) throws IOException {
        UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u -> u
                .index(EXERCISE_INDEX).id(source +HYPHEN+ customExerciseDto.getExerciseId()+DASH+ customExerciseDto.getMemberId())
                .doc(doc));
        elasticsearchClient.update(updateRequest, Object.class);
    }

    private static Map<String, String> createUpdateDoc(CustomExerciseDto customExerciseDto) {
        Map<String,String> doc = new HashMap<>();
        doc.put(EXERCISE_NAME, customExerciseDto.getExerciseName());
        doc.put(EXERCISE_GIF, customExerciseDto.getGifUrl());
        doc.put(EXERCISE_EQUIPMENT, customExerciseDto.getExerciseEquipment().getKoreanName());
        doc.put(EXERCISE_TARGET, customExerciseDto.getExerciseTarget().name());
        return doc;
    }

    private void handleDeleteExercise(DeleteRequest deleteRequest) {
        try {
            elasticsearchClient.delete(deleteRequest);
        }catch (Exception e){
            log.error("엘라스틱 삭제중 오류 발생");
        }
    }

    private void handleUpdateExercise(CustomExerciseDto customExerciseDto, String source, Map<String, String> doc) {
        try {
            elasticUpdateRequest(customExerciseDto, source, doc);
        }catch (Exception e){
            log.error("엘라스틱 업데이트시 문제 발생");
        }
    }

    private static UpdateByQueryRequest createUpdateInterestRequest(Long memberId, boolean type) {
        return new UpdateByQueryRequest.Builder()
                .index(INTEREST_INDEX)
                .query(Query.of(q -> q.bool(b -> b.must(m -> m.term(t -> t.field(MEMBER_ID).value(memberId))))))
                .script(s -> s.inline(i -> i.source(CTX_WITH_DRAW)
                        .lang(PAINLESS)
                        .params(WRIT_DRAW, JsonData.of(type)))).build();
    }


    private void handleUpdateExerciseInterest(UpdateByQueryRequest queryRequest) {
        try {
            UpdateByQueryResponse updateByQueryResponse = elasticsearchClient.updateByQuery(queryRequest);
            log.info("탈퇴한 사용자의 관심 운동 수 ={}",updateByQueryResponse.updated());
        }catch (Exception e){
            log.error("업데이트 도중 에러 발생 = {}",e.getMessage());
        }
    }
}
