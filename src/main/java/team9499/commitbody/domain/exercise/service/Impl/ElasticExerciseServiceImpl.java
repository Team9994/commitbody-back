package team9499.commitbody.domain.exercise.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.UpdateByQueryRequest;
import co.elastic.clients.elasticsearch.core.UpdateByQueryResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.ExerciseDoc;
import team9499.commitbody.domain.exercise.domain.ExerciseInterestDoc;
import team9499.commitbody.domain.exercise.repository.CustomExerciseRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseElsInterestRepository;
import team9499.commitbody.domain.exercise.repository.ExerciseElsRepository;
import team9499.commitbody.domain.exercise.service.ElasticExerciseService;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class ElasticExerciseServiceImpl implements ElasticExerciseService {

    private final CustomExerciseRepository customExerciseRepository;
    private final ExerciseElsInterestRepository exerciseElsInterestRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final ExerciseElsRepository exerciseElsRepository;

    @Value("${cloud.aws.cdn.url}")
    private String cdnUrl;

    private final String INDEX = "exercise_index";
    private final String INTEREST_INDEX_NAME = "exercise_interest_index";

    /**
     * 커스텀 운동 저장
     */
    @Override
    public void saveExercise(Long customExerciseId) {
        CustomExercise customExercise = getCustomExercise(customExerciseId);
        ExerciseDoc exerciseDoc = new ExerciseDoc().customExercise(customExercise,getCustomGifUrl(customExercise));
        exerciseElsRepository.save(exerciseDoc);
    }

    @Override
    public void updateExercise(Long customExerciseId,String source) {
        CustomExercise customExercise = getCustomExercise(customExerciseId);

        Map<String,String> doc = new HashMap<>();
        doc.put("exerciseName",customExercise.getCustomExName());
        doc.put("gifUrl",customExercise.getCustomGifUrl());
        doc.put("exerciseEquipment",customExercise.getExerciseEquipment().getKoreanName());
        doc.put("exerciseTarget",customExercise.getExerciseTarget().name());

        HashMap<String, Object> updateBody = new HashMap<>();
        updateBody.put("doc",doc);

        try {
            UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u -> u.index(INDEX).id(source+customExercise.getId()).doc(doc));
            elasticsearchClient.update(updateRequest, Map.class);
        }catch (Exception e){
            log.error("엘라스틱 업데이트시 문제 발생");
        }
    }

    /**
     * 엘라스틱 커스텀 운동 삭제 메서드
     */
    @Override
    public void deleteExercise(Long customExerciseId,Long memberId) {
        DeleteRequest deleteRequest = DeleteRequest.of(u -> u.index(INDEX).id("custom_"+customExerciseId+"-"+memberId));
        try {
            elasticsearchClient.delete(deleteRequest);
        }catch (Exception e){
            log.error("엘라스틱 삭제중 오류 발생");
        }
    }

    @Override
    public void changeInterest(Long exerciseId, String source,String status,Long memberId) {
        ExerciseInterestDoc exerciseInterestDoc = ExerciseInterestDoc.of(source + exerciseId+"-"+memberId,memberId, exerciseId, status.equals("등록") ? true : false,false);
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
        UpdateByQueryRequest queryRequest = new UpdateByQueryRequest.Builder()
                .index(INTEREST_INDEX_NAME)
                .query(Query.of(q -> q.bool(b -> b.must(m -> m.term(t -> t.field("memberId").value(memberId))))))
                .script(s -> s.inline(i -> i.source("ctx._source.withDraw = params.writDraw")
                        .lang("painless")
                        .params("writDraw", JsonData.of(type)))).build();
        try {
            UpdateByQueryResponse updateByQueryResponse = elasticsearchClient.updateByQuery(queryRequest);
            log.info("탈퇴한 사용자의 관심 운동 수 ={}",updateByQueryResponse.updated());
        }catch (Exception e){
            log.error("업데이트 도중 에러 발생 = {}",e.getMessage());
        }
    }


    private String getCustomGifUrl(CustomExercise customExercise) {
        return customExercise.getCustomGifUrl() ==null ? "등록된 이미지 파일이 없습니다." : cdnUrl+customExercise.getCustomGifUrl();
    }

    private CustomExercise getCustomExercise(Long customExerciseId) {
        CustomExercise customExercise = customExerciseRepository.findById(customExerciseId).orElse(null);
        return customExercise;
    }
}
