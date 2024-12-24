package team9499.commitbody.domain.exercise.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.exercise.dto.ReportDto;
import team9499.commitbody.domain.record.dto.response.RecordSetsResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExerciseResponse {

    private ExerciseDto exerciseDto;
    private ReportDto reportDto;
    private Set<String> exerciseMethods;        // 운동 순서
    private Map<LocalDate, List<RecordSetsResponse>> recordSetsDtos;
    @JsonIgnore
    private boolean isInterested;

    public static ExerciseResponse of(Object obExercise,ReportDto reportDto,Set<String> exerciseMethods,
                                      Map<LocalDate, List<RecordSetsResponse>> recordSetsDtos, boolean isInterested){
        ExerciseResponseBuilder builder = ExerciseResponse.builder().exerciseMethods(exerciseMethods);
        exerciseBuilder(obExercise, isInterested, builder);
        return builder.reportDto(reportDto).recordSetsDtos(recordSetsDtos).build();
    }

    private static void exerciseBuilder(Object obExercise, boolean isInterested, ExerciseResponseBuilder builder) {
        if (obExercise instanceof Exercise){
            builder.exerciseDto(ExerciseDto.of((Exercise) obExercise, isInterested));
        }
        if (obExercise instanceof CustomExercise){
            builder.exerciseDto(ExerciseDto.of((CustomExercise) obExercise, isInterested));
        }
    }

}
