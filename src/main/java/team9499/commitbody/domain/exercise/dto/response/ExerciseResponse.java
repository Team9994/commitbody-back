package team9499.commitbody.domain.exercise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    public static ExerciseResponse of(Object obExercise,ReportDto reportDto,Set<String> exerciseMethods,
                                      Map<LocalDate, List<RecordSetsResponse>> recordSetsDtos){
        ExerciseResponseBuilder builder = ExerciseResponse.builder().exerciseMethods(exerciseMethods);
        if (obExercise instanceof Exercise){
            builder.exerciseDto(ExerciseDto.of((Exercise) obExercise,false));
        }
        return builder.reportDto(reportDto).recordSetsDtos(recordSetsDtos).build();
    }

}
