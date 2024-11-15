package team9499.commitbody.domain.exercise.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Optional;

import static team9499.commitbody.global.constants.ElasticFiled.*;

@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class SchedulingService {

    private static final String CTX_GIF = "ctx._source.gifUrl = params.gifUrl";
    private static final String TIME_OUT = "10m";
    private static final String API_KKY = "x-rapidapi-key";
    private static final String API_HOST = "x-rapidapi-host";

    private final ElasticsearchClient elasticsearchClient;
    private final ExerciseRepository exerciseRepository;
    private final ObjectMapper objectMapper;

    @Value("${api.key}")
    private String key;
    @Value("${api.url}")
    private String url;
    
    /**
     * gifurl이 새벽 3시마다 초기화 되기때문에 새벽 3시 이후에 서버에서 이미지 url을 새롭게 업데이트 하기위한 메서드
     */
    public void updateGifUrl(){
        handleSuccessResponse(fetchApiData());
    }

    /**
     * gif_url이 업데이트되고난후에 일정시간후 엘라스틱에도 gif_url 주소가 변경될수있도록 새롭게 데이터를 덮어씌운다.
     */
    public void updateElData(){
        List<Exercise> exerciseList = exerciseRepository.findAll();
        BulkRequest.Builder br = getBulkRequest(exerciseList);
        processBulkRequest(br);
    }

    private ResponseEntity<String> fetchApiData() {
        HttpEntity<String> httpEntity = getStringHttpEntity();
        String getUrl = "https://" + url + "/exercises?limit=1500&offset=0";

        return new RestTemplate().exchange(getUrl, HttpMethod.GET, httpEntity, String.class);
    }

    private HttpEntity<String> getStringHttpEntity() {
        HttpHeaders headers = settingHeader();
        return new HttpEntity<>(headers);
    }

    private HttpHeaders settingHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KKY, key);
        headers.set(API_HOST, url);
        return headers;
    }

    private void handleSuccessResponse(ResponseEntity<String> response) {
        if (response.getStatusCode().is2xxSuccessful()){
            try{
                parseAndSaveExercises(response);
            }catch (Exception e){
                throw new ServerException(ExceptionStatus.INTERNAL_SERVER_ERROR, ExceptionType.SERVER_ERROR);
            }
        }
    }

    private void parseAndSaveExercises(ResponseEntity<String> response) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        if (jsonNode.isArray()){
            List<Exercise> exercisesToUpdate = getExercises(jsonNode);
            exerciseSave(exercisesToUpdate);
        }
    }


    private List<Exercise> getExercises(JsonNode jsonNode) {
        List<Exercise> exercisesToUpdate = new ArrayList<>();
        for (JsonNode node : jsonNode){
            String gifUrl = node.get(EXERCISE_GIF).asText();
            long id = node.get(ID).asLong();

            Optional<Exercise> optionalExercise = exerciseRepository.findById(id);
            if (optionalExercise.isPresent()) {
                Exercise exercise = optionalExercise.get();
                exercise.updateGifUrl(gifUrl);
                exercisesToUpdate.add(exercise);
            }
        }
        return exercisesToUpdate;
    }

    private void exerciseSave(List<Exercise> exercisesToUpdate) {
        if (!exercisesToUpdate.isEmpty()) {
            exerciseRepository.saveAll(exercisesToUpdate);
        }
    }

    private static Time getTimeOutBuilder() {
        return new Time.Builder().time(TIME_OUT).build();
    }

    private static BulkRequest.Builder getBulkRequest(List<Exercise> exerciseList) {
        BulkRequest.Builder builder = new BulkRequest.Builder();
        // 기본 운동 목록
        for (Exercise exercise : exerciseList) {
            builder.operations(op -> op.update(u -> u
                    .index(EXERCISE_INDEX)
                    .id(DEFAULT_+exercise.getId())
                    .action(a -> a.script(s ->
                            s.inline(i -> i
                                    .source(CTX_GIF)
                                    .lang(PAINLESS)
                                    .params(EXERCISE_GIF, JsonData.of(exercise.getGifUrl())))))));
        }
        return builder;
    }

    private void processBulkRequest(BulkRequest.Builder br) {
        try {
            elasticsearchClient.bulk(br.timeout(getTimeOutBuilder()).build());
        }catch (Exception e){
            log.error("운동 gif 업데이트 중 오류 발생 ={}",e.getMessage());
        }
    }
}
