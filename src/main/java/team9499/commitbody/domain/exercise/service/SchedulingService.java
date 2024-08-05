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
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.repository.ExerciseRepository;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.ServerException;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SchedulingService {

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
                throw new ServerException(ExceptionStatus.SERVER_ERROR, ExceptionType.SERVER_ERROR);
            }

        }
    }
}
