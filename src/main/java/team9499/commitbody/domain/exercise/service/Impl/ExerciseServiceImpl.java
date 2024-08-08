package team9499.commitbody.domain.exercise.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final String NAME_FIELD = "exerciseName";
    private final String EQUIPMENT_FIELD = "exerciseEquipment";
    private final String TARGET_FIELD = "exerciseTarget";
    private final String FAVORITES_FILED = "favorites";
    private final String MEMBER_FILED = "memberId";
    private final String SCORE_FILED = "_score";


    /**
     * 운동 검색을 위한 메서드
     * @param name  운동명
     * @param target    운동 부위
     * @param equipment 운동 장비
     * @param from      페이지
     * @param size      페이지 사이즈
     * @param favorites 관심 운동
     * @param memberId  로그인한 사용자 ID
     */
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

        // 관심운동의 대한 동적 쿼리
        if (favorites!=null){
            TermQuery termQuery = new TermQuery.Builder()
                    .field(FAVORITES_FILED)
                    .value(favorites).build();
            boolQueryBuilder.filter(Query.of(builder ->  builder.term(termQuery)));
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


        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .query(Query.of(q -> q.bool(boolQueryBuilder.minimumShouldMatch(String.valueOf(1)).build())))       // bool 최소 만족 조건 1 -> or
                .size(size)
                .from(from)
                .sort(SortOptions.of(s -> s.field(f -> f.field(SCORE_FILED).order(SortOrder.Asc)))).build();       // 사용자가 추가한 운동은 맨 아래로

        try {
            SearchResponse<Object> searchResponse = elasticsearchClient.search(searchRequest, Object.class);
            long value = searchResponse.hits().total().value();     // 검색한 총 데이터 수

            List<Map<String, Object>> values = searchResponse.hits().hits().stream()
                    .map(hit -> (Map<String, Object>) hit.source())
                    .collect(Collectors.toList());

            return new SearchExerciseResponse(value,values);
        }catch (Exception e){
            throw new ServerException(ExceptionStatus.SERVER_ERROR, ExceptionType.SERVER_ERROR);
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
