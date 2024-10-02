package team9499.commitbody.domain.exercise.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.json.JsonData;
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

@Slf4j
@Service
@Transactional(transactionManager = "dataTransactionManager")
@RequiredArgsConstructor
public class SchedulingService {

    private final ElasticsearchClient elasticsearchClient;
    private final ExerciseRepository exerciseRepository;
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
                    List<Exercise> exercisesToUpdate = new ArrayList<>();
                    for (JsonNode node : jsonNode){
                        String gifUrl = node.get("gifUrl").asText();
                        long id = node.get("id").asLong();

                        Optional<Exercise> optionalExercise = exerciseRepository.findById(id);
                        if (optionalExercise.isPresent()) {
                            Exercise exercise = optionalExercise.get();
                            exercise.updateGifUrl(gifUrl);
                            exercisesToUpdate.add(exercise);
                        }
                    }
                    if (!exercisesToUpdate.isEmpty()) {
                        exerciseRepository.saveAll(exercisesToUpdate);
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

        BulkRequest.Builder br = new BulkRequest.Builder();

        // 기본 운동 목록
        for (Exercise exercise : exerciseList) {
            br.operations(op -> op.update(u -> u
                    .index("exercise_index")
                    .id(DEFAULT+exercise.getId())
                    .action(a -> a.script(s ->
                            s.inline(i -> i
                                    .source("ctx._source.gifUrl = params.gifUrl")
                                    .lang("painless")
                                    .params("gifUrl", JsonData.of(exercise.getGifUrl())))))));
        }

        try {
            Time time = new Time.Builder().time("10m").build();
            elasticsearchClient.bulk(br.timeout(time).build());
        }catch (Exception e){
            log.error("운동 gif 업데이트 중 오류 발생 ={}",e.getMessage());
        }

    }
}
