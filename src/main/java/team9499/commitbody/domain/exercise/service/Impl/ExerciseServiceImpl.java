package team9499.commitbody.domain.exercise.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.Member.domain.Member;
import team9499.commitbody.domain.Member.repository.MemberRepository;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.exercise.dto.CustomExerciseDto;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.exercise.dto.SearchExerciseResponse;
import team9499.commitbody.domain.exercise.dto.response.ExerciseResponse;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseInterestRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.domain.exercise.service.ElasticExerciseInterestService;
import team9499.commitbody.domain.exercise.service.ExerciseInterestService;
import team9499.commitbody.domain.exercise.service.ExerciseService;
import team9499.commitbody.domain.like.repository.LikeRepository;
import team9499.commitbody.domain.record.repository.RecordRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.Exception.ServerException;
import team9499.commitbody.global.aws.s3.S3Service;
import team9499.commitbody.global.redis.RedisService;

import java.util.*;

import static team9499.commitbody.global.utils.ElasticFiled.*;

@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {
    
    private final ElasticsearchClient elasticsearchClient;
    private final CustomExerciseRepository customExerciseRepository;
    private final LikeRepository commentLikeRepository;
    private final RecordRepository recordRepository;
    private final ExerciseInterestRepository exerciseInterestRepository;
    private final ExerciseInterestService exerciseInterestService;
    private final ExerciseRepository exerciseRepository;
    private final RedisService redisService;
    private final MemberRepository memberRepository;
    private final ElasticExerciseInterestService exerciseFavoriteService;
    private final S3Service s3Service;

    @Value("${cloud.aws.cdn.url}")
    private String CDN_RUL;

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
        
        buildExerciseNameQuery(name, boolQueryBuilder);
        buildExerciseEquipmentQuery(equipment, boolQueryBuilder);
        buildTagetQuery(target, EXERCISE_TARGET, boolQueryBuilder);
        buildExerciseTypeQuery(exerciseType, boolQueryBuilder);
        addCurrentUserFilter(memberId, boolQueryBuilder); // 현재 사용자의 대해서만 조회

        List<SortOptions> options = List.of(
                SortOptions.of(s -> s.field(f -> f.field(_SCORE).order(SortOrder.Asc))),
                SortOptions.of(s -> s.field(f -> f.field(_SOURCE).field(EXERCISE_ID).order(SortOrder.Asc))),
                SortOptions.of(s -> s.field(f -> f.field(_SCORE).order(SortOrder.Asc)))
        );

        // 관심운동의 대한 요청 처리
        if (favorites!=null){
            return exerciseFavoriteService.searchFavoriteExercise(memberId,boolQueryBuilder,size,from,options);
        }

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(EXERCISE_INDEX)
                .query(Query.of(q -> q.bool(boolQueryBuilder.minimumShouldMatch(String.valueOf(1)).build())))       // bool 최소 만족 조건 1 -> or
                .size(size)
                .from(from)
                .sort(options)
                .build();       // 사용자가 추가한 운동은 맨 아래로

        try {
            SearchResponse<Object> searchResponse = elasticsearchClient.search(searchRequest, Object.class);
            long value = searchResponse.hits().total() != null ? searchResponse.hits().total().value() : 0;     // 검색한 총 데이터 수

            List<ExerciseDto> response = searchResponse.hits().hits().stream()
                    .map(hit -> {
                        Map<String, Object> source = (Map<String, Object>) hit.source();
                        Long exerciseId = Long.valueOf(source.get(EXERCISE_ID).toString());
                        String exName = (String) source.get(EXERCISE_NAME);
                        String gifUrl = Optional.ofNullable(source.get(EXERCISE_GIF)).map(Object::toString).orElse("등록된 이미지 파일이 없습니다.");
                        String exTarget = (String) source.get(EXERCISE_TARGET);
                        String type = source.get(EXERCISE_TYPE) !=null ? source.get(EXERCISE_TYPE).toString() : "무게와 횟수";
                        String exEquipment = (String) source.get(EXERCISE_EQUIPMENT);
                        String src = (String) source.get(SOURCE);
                        return ExerciseDto.of(exerciseId, exName, gifUrl, exTarget, type, exEquipment, src, false);
                    })
                    .toList();

            response = exerciseFavoriteService.updateInterestFieldStatus(memberId, response);

            return new SearchExerciseResponse(value,response);
        }catch (Exception e){
            log.error("운동 검색중 오류 발생 ={}",e.getMessage());
            throw new ServerException(ExceptionStatus.INTERNAL_SERVER_ERROR, ExceptionType.SERVER_ERROR);
        }
    }


    /**
     * 커스텀 운동등록 메서드
     * 이미지는 s3에 업로드
     */
    @Override
    public CustomExerciseDto saveCustomExercise(String exerciseName, ExerciseTarget exerciseTarget, ExerciseEquipment exerciseEquipment,Long memberId, MultipartFile file) {
        String storedFileName = s3Service.uploadFile(file);
        Optional<Member> redisMember = getRedisMember(memberId);

        CustomExercise customExercise = new CustomExercise().save(exerciseName, storedFileName, exerciseTarget, exerciseEquipment,redisMember.get());
        CustomExercise exercise = customExerciseRepository.save(customExercise);
        return CustomExerciseDto.fromDto(exercise, getImgUrl(storedFileName));
    }

    /**
     * 커스텀 운동 업데이트 메서드
     */
    @Override
    public CustomExerciseDto updateCustomExercise(String exerciseName, ExerciseTarget exerciseTarget, ExerciseEquipment exerciseEquipment, Long memberId, Long customExerciseId, MultipartFile file) {
        CustomExercise customExercise = getCustomExercise(customExerciseId,memberId);
        String updateImage = s3Service.updateFile(file, customExercise.getCustomGifUrl());
        customExercise.update(exerciseName,exerciseTarget,exerciseEquipment,updateImage);
        return CustomExerciseDto.fromDto(customExercise,getImgUrl(updateImage));
    }

    /**
     * DB 커스텀 운동 삭제 메서드
     */
    @Override
    public void deleteCustomExercise(Long customExerciseId, Long memberId) {
        CustomExercise customExercise = getCustomExercise(customExerciseId,memberId);
        recordRepository.deleteCustomExercise(customExercise.getId());                      // 운동 기록 삭제
        exerciseInterestRepository.deleteAllByCustomExerciseId(customExercise.getId());     // 관심 운동 삭제
        commentLikeRepository.deleteByCustomExerciseId(customExercise.getId());     // 댓글 좋아요 삭제
        customExerciseRepository.delete(customExercise);                    // 커스텀 운동 삭제
    }

    /**
     * 상세 운동 조회하는 메서드
     * @param exerciseId 운동 ID
     * @param memberId 로그인한 사용자 ID
     * @param source 운동 정보 타입 [custom,default]
     */
    @Transactional(readOnly = true)
    @Override
    public ExerciseResponse detailsExercise(Long exerciseId, Long memberId, String source) {
        ExerciseResponse exerciseDetailReport = exerciseRepository.getExerciseDetailReport(memberId, exerciseId, source);
        boolean interestStatus = exerciseInterestService.checkInterestStatus(exerciseId, memberId);
        exerciseDetailReport.setInterestStatus(interestStatus);

        return exerciseDetailReport;
    }

    private void addCurrentUserFilter(String memberId, BoolQuery.Builder boolQueryBuilder) {
        boolQueryBuilder.should(Query.of(
                        q -> q.bool(
                                b -> b.mustNot(
                                        m ->m.exists(
                                                e -> e.field(MEMBER_FILED)
                                        )
                                )
                        )))
                .should(Query.of(S -> S.term(t -> t.field(MEMBER_FILED).value(memberId))));
    }

    private static void buildExerciseTypeQuery(String exerciseType, BoolQuery.Builder boolQueryBuilder) {
        if (exerciseType !=null && !exerciseType.isEmpty()){
            TermQuery termQuery = new TermQuery.Builder()
                    .field(SOURCE)
                    .value(exerciseType).build();
            boolQueryBuilder.filter(Query.of(q -> q.term(termQuery)));
        }
    }

    private void buildTagetQuery(String target, String TARGET_FIELD, BoolQuery.Builder boolQueryBuilder) {
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
                    .query("*" + name + "*")  // 와일드카드를 사용한 검색 쿼리  정확성이 조금 낮음
                    .fields(EXERCISE_NAME)
                    .defaultOperator(Operator.And).build();
            boolQueryBuilder.must(Query.of(q -> q.queryString(queryStringQuery)));
        }
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
        return customExerciseRepository.findByIdAndAndMemberId(customExerciseId,memberId).orElseThrow(() -> new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA));
    }


    private String getImgUrl(String storedFileName) {
        return storedFileName != null ? CDN_RUL + storedFileName : null;
    }
}
