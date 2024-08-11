package team9499.commitbody.domain.exercise.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Transactional
@RequiredArgsConstructor
public class ElasticExerciseServiceImpl implements ElasticExerciseService {

    private final CustomExerciseRepository customExerciseRepository;
    private final ExerciseElsInterestRepository exerciseElsInterestRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final ExerciseElsRepository exerciseElsRepository;

    @Value("${cloud.aws.cdn.url}")
    private String cdnUrl;

    private final String INDEX = "exercise_index";

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
    public void deleteExercise(Long customExerciseId) {
        DeleteRequest deleteRequest = DeleteRequest.of(u -> u.index(INDEX).id("custom_"+customExerciseId));
        try {
            elasticsearchClient.delete(deleteRequest);
        }catch (Exception e){
            log.error("엘라스틱 삭제중 오류 발생");
        }
    }

    @Override
    public void changeInterest(Long exerciseId, String source,String status,Long memberId) {

        ExerciseInterestDoc exerciseInterestDoc = new ExerciseInterestDoc(source + exerciseId+"-"+memberId,memberId, exerciseId, status.equals("등록") ? true : false);
        exerciseElsInterestRepository.save(exerciseInterestDoc);

    }

    private String getCustomGifUrl(CustomExercise customExercise) {
        return customExercise.getCustomGifUrl() ==null ? "등록된 이미지 파일이 없습니다." : cdnUrl+customExercise.getCustomGifUrl();
    }

    private CustomExercise getCustomExercise(Long customExerciseId) {
        CustomExercise customExercise = customExerciseRepository.findById(customExerciseId).orElse(null);
        return customExercise;
    }
}
