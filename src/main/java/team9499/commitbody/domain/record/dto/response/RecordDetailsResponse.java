package team9499.commitbody.domain.record.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;
import team9499.commitbody.domain.record.domain.RecordDetails;


import java.util.ArrayList;
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
    private String exerciseName;
    private ExerciseType exerciseType;
    private String gifUrl;              // gif주소
    private Integer detailsReps;        // 횟수
    private Integer detailsSets;        // 세트수
    private Integer detailsTimes;       // 누적시간
    private Integer detailsVolume;
    private Integer maxReps;
    private Integer max1RM;
    private List<RecordSetsResponse> sets;

    public static RecordDetailsResponse of(RecordDetails recordDetails, Exercise exercise, CustomExercise customExercise){
        RecordDetailsResponseBuilder responseBuilder = createInitBuilder(recordDetails);
        exerciseTypeBuilder(exercise, customExercise, responseBuilder);
        detailsBuilder(recordDetails, responseBuilder);
        return responseBuilder.build();
    }

    private static RecordDetailsResponseBuilder createInitBuilder(RecordDetails recordDetails) {
        return RecordDetailsResponse
                .builder()
                .recordDetailId(recordDetails.getId())
                .detailsSets(recordDetails.getDetailsSets())
                .sets(new ArrayList<>());
    }

    private static void exerciseTypeBuilder(Exercise exercise, CustomExercise customExercise, RecordDetailsResponseBuilder responseBuilder) {
        if (exercise !=null){
            responseBuilder.exerciseId(exercise.getId()).exerciseName(exercise.getExerciseName())
                    .exerciseType(exercise.getExerciseType()).gifUrl(exercise.getGifUrl());
            return;
        }
        responseBuilder.customExerciseId(customExercise.getId());
        if (customExercise.getCustomGifUrl()!=null){
            responseBuilder.gifUrl(customExercise.getCustomGifUrl());
        }
    }

    private static void detailsBuilder(RecordDetails recordDetails, RecordDetailsResponseBuilder responseBuilder) {
        if (recordDetails.getSumTimes()!=null){
            responseBuilder.detailsTimes(recordDetails.getSumTimes());
            return;
        }
        if (recordDetails.getMaxReps()!=null){
            responseBuilder.detailsReps(recordDetails.getDetailsReps()).maxReps(recordDetails.getMaxReps());
            return;
        }
        responseBuilder.detailsReps(recordDetails.getDetailsReps()).detailsVolume(recordDetails.getDetailsVolume())
                .max1RM(recordDetails.getMax1RM());
    }
}
