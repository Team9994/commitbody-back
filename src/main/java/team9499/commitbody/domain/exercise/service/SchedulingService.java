package team9499.commitbody.domain.exercise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import team9499.commitbody.domain.exercise.domain.*;
import team9499.commitbody.domain.exercise.repository.*;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.ServerException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SchedulingService {

    private final ExerciseRepository exerciseRepository;
    private final CustomExerciseRepository customExerciseRepository;
    private final ExerciseElsRepository exerciseElsRepository;
    private final ExerciseInterestRepository exerciseInterestRepository;
    private final ExerciseElsInterestRepository exerciseElsInterestRepository;
    private final ObjectMapper objectMapper;

    @Value("${api.key}")
    private String key;
    @Value("${api.url}")
    private String url;

    private final String DEFAULT = "default_";
    private final String CUSTOM = "custom_";
    /**
     * gifurl이 새벽 3시마다 초기화 되기때문에 새벽 3시 이후에 서버에서 이미지 url을 새롭게 업데이트 하기위한 메서드
     */
    public void updateGifUrl(){
        RestTemplate rest = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-key", key);
        headers.set("x-rapidapi-host", url);

        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        String getUrl = "https://"+url+"/exercises?limit=1500&offset=0";

        ResponseEntity<String> response = rest.exchange(getUrl, HttpMethod.GET, httpEntity, String.class);

        if (response.getStatusCode().is2xxSuccessful()){
            try{
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                if (jsonNode.isArray()){
                    for (JsonNode node : jsonNode){
                        String gifUrl = node.get("gifUrl").asText();
                        long id = node.get("id").asLong();
                        boolean existsById = exerciseRepository.existsById(id);
                        if (existsById){
                            Exercise exercise = exerciseRepository.findById(id).get();
                            exercise.updateGifUrl(gifUrl);
                        }
                    }
                }
            }catch (Exception e){
                throw new ServerException(ExceptionStatus.INTERNAL_SERVER_ERROR, ExceptionType.SERVER_ERROR);
            }

        }
    }

    /**
     * gif_url이 업데이트되고난후에 일정시간후 엘라스틱에도 gif_url 주소가 변경될수있도록 새롭게 데이터를 덮어씌운다.
     */
    public void updateElData(){
        List<Exercise> exerciseList = exerciseRepository.findAll();
        List<CustomExercise> customExercises = customExerciseRepository.findAll();
        List<ExerciseInterest> exerciseInterests = exerciseInterestRepository.findAll();
        List<ExerciseDoc> exerciseDocList = new ArrayList<>();
        List<ExerciseInterestDoc> exerciseInterestDocList = new ArrayList<>();

        // 기본 운동 목록
        for (Exercise exercise : exerciseList) {
            ExerciseDoc aDefault = new ExerciseDoc(DEFAULT+exercise.getId(),exercise.getId(), exercise.getExerciseName(), exercise.getGifUrl(), exercise.getExerciseTarget().name(),
                    exercise.getExerciseType().getDescription(), exercise.getExerciseEquipment().getKoreanName(), null, "default", false);
            exerciseDocList.add(aDefault);
        }
        // 커스텀 운동 목록
        for (CustomExercise customExercise : customExercises) {
            ExerciseDoc custom = new ExerciseDoc(CUSTOM+customExercise.getId(),customExercise.getId(), customExercise.getCustomExName(), customExercise.getCustomGifUrl(), customExercise.getExerciseTarget().name(), null, customExercise.getExerciseEquipment().getKoreanName(), String.valueOf(customExercise.getMember().getId()), "custom",false);
            exerciseDocList.add(custom);
        }

        // 관심 운동 목록 업데이트
        for (ExerciseInterest exerciseInterest : exerciseInterests) {
            Long memberId = exerciseInterest.getMember().getId();
            String id;
            Long exerciseId = (exerciseInterest.getExercise() != null) ? exerciseInterest.getExercise().getId() : exerciseInterest.getCustomExercise().getId();
            id = (exerciseInterest.getExercise() != null ? DEFAULT : CUSTOM) + exerciseId + memberId;
            exerciseInterestDocList.add(new ExerciseInterestDoc(id, memberId, exerciseId, exerciseInterest.isInterested()));
        }

        exerciseElsRepository.saveAll(exerciseDocList);
        exerciseElsInterestRepository.saveAll(exerciseInterestDocList);
    }
}
