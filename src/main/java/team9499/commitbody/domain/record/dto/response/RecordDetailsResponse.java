package team9499.commitbody.domain.record.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;


import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordDetailsResponse {

    private Long recordDetailId;
    private Long exerciseId;
    private Long customExerciseId;
    private String gifUrl;              // gif주소
    private Integer detailsReps;        // 횟수
    private Integer detailsSets;        // 세트수
    private Integer detailsTimes;       // 누적시간
    private Integer detailsVolume;
    private Integer maxReps;
    private Integer max1RM;
    private List<RecordSetsResponse> sets;

    public static RecordDetailsResponse of(Long recordDetailId, Exercise exercise, CustomExercise customExercise, Integer detailsReps, Integer detailsSets, Integer detailsTimes, Integer detailsVolume, Integer maxReps, Integer max1RM, List<RecordSetsResponse> sets){
        RecordDetailsResponseBuilder recordDetailsResponseBuilder = RecordDetailsResponse.builder().recordDetailId(recordDetailId).detailsSets(detailsSets);
        if(exercise!=null){
            recordDetailsResponseBuilder.exerciseId(exercise.getId()).gifUrl(exercise.getGifUrl());
        }else{
            recordDetailsResponseBuilder.customExerciseId(customExercise.getId());
            if (customExercise.getCustomGifUrl() != null){
                recordDetailsResponseBuilder.gifUrl(customExercise.getCustomGifUrl());
            }
        }


        if (detailsTimes!=null){
            recordDetailsResponseBuilder.detailsTimes(detailsTimes);
        } else if (maxReps!=null) {
            recordDetailsResponseBuilder.detailsReps(detailsReps).maxReps(maxReps);
        }else {
            recordDetailsResponseBuilder.detailsReps(detailsReps).detailsVolume(detailsVolume).max1RM(max1RM);
        }

        return recordDetailsResponseBuilder.build();

    }
}
