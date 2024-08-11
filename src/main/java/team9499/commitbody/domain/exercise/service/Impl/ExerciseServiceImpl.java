package team9499.commitbody.domain.exercise.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.dto.SearchExerciseResponse;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.service.ExerciseService;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.Exception.ServerException;
import team9499.commitbody.global.aws.s3.S3Service;
import team9499.commitbody.global.redis.RedisService;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {
    
    private final ElasticsearchClient elasticsearchClient;
    private final CustomExerciseRepository customExerciseRepository;
    private final RedisService redisService;
    private final MemberRepository memberRepository;
    private final S3Service s3Service;

    private final String INDEX_NAME = "exercise_index";
    private final String INTEREST_INDEX_NAME = "exercise_interest_index";
    private final String EXERCISE_ID = "exerciseId";
    private final String NAME_FIELD = "exerciseName";
    private final String EQUIPMENT_FIELD = "exerciseEquipment";
    private final String GIF_URL ="gifUrl";
    private final String TARGET_FIELD = "exerciseTarget";
    private final String INTEREST_FILED = "interest";
    private final String EXERCISE_TYPE ="exerciseType";
    private final String MEMBER_FILED = "memberId";
    private final String SCORE_FILED = "_score";
    private final String SOURCE_FILED = "_source";


    /**
     * 운동 검색을 위한 메서드
     * @param name  운동명
     * @param target    운동 부위
     * @param equipment 운동 장비
     * @param from      페이지
     * @param size      페이지 사이즈
     * @param memberId  로그인한 사용자 ID
     */
    // TODO: 2024-08-12 추후의 코드 리팩토링 진행 너무 가독성이 좋지 않다.
    @Override
    public SearchExerciseResponse searchExercise(String name, String target, String equipment, Integer from, Integer size, Boolean favorites, String memberId) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // 운동명의 대한 동적 쿼리
        if (name!=null&& !name.isEmpty()) {
            QueryStringQuery queryStringQuery = new QueryStringQuery.Builder()
                    .query("*" + name + "*")  // 와일드카드를 사용한 검색 쿼리  정확성이 조금 낮음
                    .fields(NAME_FIELD)
                    .defaultOperator(Operator.And).build();
            boolQueryBuilder.must(Query.of(q -> q.queryString(queryStringQuery)));
        }

        if (equipment!=null && !equipment.isEmpty()){
            TermQuery termQuery = new TermQuery.Builder()
                    .field(EQUIPMENT_FIELD)
                    .value(equipment).build();
            boolQueryBuilder.filter(Query.of(q-> q.term(termQuery)));
        }

        // 운동부위의 대한 동적 쿼리
        if (target != null && !target.isEmpty()) {
            TermQuery termQuery = new TermQuery.Builder()
                    .field(TARGET_FIELD)
                    .value(target).build();
            boolQueryBuilder.filter(Query.of(q -> q.term(termQuery)));
        }



        // 현재 사용자의 대해서만 조회
        boolQueryBuilder.should(Query.of(
                q -> q.bool(
                        b -> b.mustNot(
                                m ->m.exists(
                                        e -> e.field(MEMBER_FILED)
                                )
                        )
                )))
                .should(Query.of(S -> S.term(t -> t.field(MEMBER_FILED).value(memberId))));


        List<SortOptions> objects = List.of(
                SortOptions.of(s -> s.field(f -> f.field(SCORE_FILED).order(SortOrder.Asc))),
                SortOptions.of(s -> s.field(f -> f.field(SOURCE_FILED).field(EXERCISE_ID).order(SortOrder.Asc))),
                SortOptions.of(s -> s.field(f -> f.field(SCORE_FILED).order(SortOrder.Asc)))
        );

        // 관심운동의 대한 동적 쿼리
        if (favorites!=null){
            BoolQuery favoriteQuery = new BoolQuery.Builder()
                    .must(m -> m.term(t -> t.field(MEMBER_FILED).value(memberId))).build();

            SearchRequest favoriteRequest = new SearchRequest.Builder()
                    .index(INTEREST_INDEX_NAME)
                    .query(q -> q.bool(favoriteQuery))
                    .build();

            try {
                SearchResponse<Object> favoriteResponse = elasticsearchClient.search(favoriteRequest, Object.class);
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

                boolQueryBuilder.must(Query.of(q -> q.bool(favoriteFilterQuery)));

                SearchRequest searchRequest = new SearchRequest.Builder()
                        .index(INDEX_NAME)
                        .query(Query.of(q -> q.bool(boolQueryBuilder.minimumShouldMatch(String.valueOf(1)).build())))
                        .size(size)
                        .from(from)
                        .sort(objects)
                        .build();

                SearchResponse<Object> searchResponse = elasticsearchClient.search(searchRequest, Object.class);

                long value = searchResponse.hits().total().value();     // 검색한 총 데이터 수

                List<LinkedHashMap<String,Object>> response = new LinkedList<>();
                List<Hit<Object>> hits1 = searchResponse.hits().hits();
                List<Long> ids = new ArrayList<>();
                for (Hit<Object> hit : hits1) {
                    LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>();
                    Map<String,Object> source = (Map<String,Object>)hit.source();
                    linkedHashMap.put(EXERCISE_ID,source.get(EXERCISE_ID));
                    linkedHashMap.put(NAME_FIELD,source.get(NAME_FIELD));
                    linkedHashMap.put(GIF_URL,source.get(GIF_URL));
                    linkedHashMap.put(TARGET_FIELD,source.get(TARGET_FIELD));
                    linkedHashMap.put(EXERCISE_TYPE,"무게와 횟수");
                    linkedHashMap.put(EQUIPMENT_FIELD,source.get(EQUIPMENT_FIELD));
                    linkedHashMap.put("source",source.get("source"));
                    linkedHashMap.put(INTEREST_FILED,true);
                    response.add(linkedHashMap);
                    ids.add(Long.valueOf(source.get(EXERCISE_ID).toString()));
                }

                return new SearchExerciseResponse(value,response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .query(Query.of(q -> q.bool(boolQueryBuilder.minimumShouldMatch(String.valueOf(1)).build())))       // bool 최소 만족 조건 1 -> or
                .size(size)
                .from(from)
                .sort(objects)
                .build();       // 사용자가 추가한 운동은 맨 아래로

        try {
            SearchResponse<Object> searchResponse = elasticsearchClient.search(searchRequest, Object.class);
            long value = searchResponse.hits().total().value();     // 검색한 총 데이터 수

            List<LinkedHashMap<String,Object>> response = new LinkedList<>();
            List<Hit<Object>> hits = searchResponse.hits().hits();
            List<Long> ids = new ArrayList<>();
            for (Hit<Object> hit : hits) {
                LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>();
                Map<String,Object> source = (Map<String,Object>)hit.source();
                linkedHashMap.put(EXERCISE_ID,source.get(EXERCISE_ID));
                linkedHashMap.put(NAME_FIELD,source.get(NAME_FIELD));
                linkedHashMap.put(GIF_URL,source.get(GIF_URL));
                linkedHashMap.put(TARGET_FIELD,source.get(TARGET_FIELD));
                linkedHashMap.put(EXERCISE_TYPE,source.get(EXERCISE_TYPE));
                linkedHashMap.put(EQUIPMENT_FIELD,source.get(EQUIPMENT_FIELD));
                linkedHashMap.put("source",source.get("source"));
                linkedHashMap.put(INTEREST_FILED,false);
                response.add(linkedHashMap);
                ids.add(Long.valueOf(source.get(EXERCISE_ID).toString()));
            }

            TermsQueryField countryTerms = new TermsQueryField.Builder()
                    .value(ids.stream().map(FieldValue::of).toList())
                    .build();

            BoolQuery interestBool = new BoolQuery.Builder()
                    .must(m -> m.term(t -> t.field(MEMBER_FILED).value(memberId)))
                    .must(m -> m.terms(t -> t.field(EXERCISE_ID).terms(countryTerms))).build();

            SearchRequest interestRequest = new SearchRequest.Builder()
                    .index(INTEREST_INDEX_NAME)
                    .query(q -> q.bool(interestBool))       // bool 최소 만족 조건 1 -> or
                    .build();

            SearchResponse<Object> interestResponse = elasticsearchClient.search(interestRequest, Object.class);
            List<Hit<Object>> interestHits = interestResponse.hits().hits();

            for (Hit<Object> hit :interestHits){
                Map<String,Object> source = (Map<String,Object>)hit.source();
                String exerciseId = source.get(EXERCISE_ID).toString();     // 아이디값 추출
                Boolean status = (Boolean) source.get("status");            // 관심 상태 추출
                for (LinkedHashMap<String,Object> val : response){
                    for (String idKey : val.keySet()){              // map을 반복
                        if (idKey.equals(EXERCISE_ID)){             //  관심한 운동의 id랑 조회한 운동 id랑 같다면
                            String valueInResponse = val.get(idKey).toString().trim();
                            if (exerciseId.equals(valueInResponse)){
                                val.put(INTEREST_FILED,status);        // 상태변경
                                break;
                            }
                        }
                    }
                }
            }

            return new SearchExerciseResponse(value,response);
        }catch (Exception e){
            e.printStackTrace();
            throw new ServerException(ExceptionStatus.INTERNAL_SERVER_ERROR, ExceptionType.SERVER_ERROR);
        }
    }

    /**
     * 커스텀 운동등록 메서드
     * 이미지는 s3에 업로드
     */
    @Override
    public Long saveCustomExercise(String exerciseName, ExerciseTarget exerciseTarget, ExerciseEquipment exerciseEquipment,Long memberId, MultipartFile file) {
        String storedFileName = s3Service.uploadImage(file);
        Optional<Member> redisMember = getRedisMember(memberId);

        CustomExercise customExercise = new CustomExercise().save(exerciseName, storedFileName, exerciseTarget, exerciseEquipment,redisMember.get());
        CustomExercise exercise = customExerciseRepository.save(customExercise);
        return exercise.getId();
    }

    /**
     * 커스텀 운동 업데이트 메서드
     */
    @Override
    public Long updateCustomExercise(String exerciseName, ExerciseTarget exerciseTarget, ExerciseEquipment exerciseEquipment, Long memberId, Long customExerciseId, MultipartFile file) {
        CustomExercise customExercise = getCustomExercise(customExerciseId,memberId);
        String updateImage = s3Service.updateImage(file, customExercise.getCustomGifUrl());
        customExercise.update(exerciseName,exerciseTarget,exerciseEquipment,updateImage);
        return customExercise.getId();
    }

    /**
     * DB 커스텀 운동 삭제 메서드
     */
    @Override
    public void deleteCustomExercise(Long customExerciseId, Long memberId) {
        CustomExercise customExercise = getCustomExercise(customExerciseId,memberId);
        customExerciseRepository.delete(customExercise);
    }


    private Optional<Member> getRedisMember(Long memberId) {
        Optional<Member> optionalMember = redisService.getMemberDto(String.valueOf(memberId));

        if (optionalMember.isEmpty()){
            Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.No_SUCH_MEMBER));
            optionalMember = Optional.of(member);
        }
        return optionalMember;
    }

    private CustomExercise getCustomExercise(Long customExerciseId, Long memberId) {
        CustomExercise customExercise = customExerciseRepository.findByIdAndAndMemberId(customExerciseId,memberId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
        return customExercise;
    }

}
