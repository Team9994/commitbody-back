package team9499.commitbody.domain.exercise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExerciseResponse {

    private Long exerciseId;            // 운동 ID
    private String exerciseName;        // 운동명
    private ExerciseTarget exerciseTarget;  // 운동 부위
    private boolean interestStatus;         // 관심운동 상태
    private String exerciseEquipment;       // 운동 장비
    private String exerciseType;            // 운동 수행 종류
    private String gifUrl;                  // 움짤
    private Integer totalValue;             // 해당 운동의 총합 [횟수기반, 횟수+무게기반, 시간기반]
    private Integer maxValue;               // 해당 운동 최댓 값 [횟수기반, 횟수+무게기반, 시간기반]
    private Integer weekValue;              // 주간 운동의 총합 [횟수기반, 횟수+무게기반, 시간기반]
    private Integer calculateRankPercentage;    // 상위 기록 (추후 구현)
    private Map<DayOfWeek, Integer> day;        // 주간 수행한 데이터
    private Set<String> exerciseMethods;        // 운동 순서 
    private List<ExerciseDetailsResponse> records;  // 날짜별 운동 세트수

    public static ExerciseResponse of(Object obExercise,Integer maxValue,Integer totalValue,Integer weekValue,Map<DayOfWeek, Integer> day,Set<String> exerciseMethods,List<ExerciseDetailsResponse> records){
        ExerciseResponseBuilder builder = ExerciseResponse.builder().day(day).maxValue(maxValue).totalValue(totalValue).calculateRankPercentage(0).weekValue(weekValue).exerciseMethods(exerciseMethods).records(records);
        if (obExercise instanceof Exercise){
            Exercise exercise = (Exercise) obExercise;
            builder.exerciseId(exercise.getId()).exerciseName(exercise.getExerciseName()).exerciseType(exercise.getExerciseType().getDescription()).exerciseTarget(exercise.getExerciseTarget()).exerciseEquipment(exercise.getExerciseEquipment().getKoreanName()).gifUrl(exercise.getGifUrl());
        }else{
            CustomExercise customExercise = (CustomExercise) obExercise;
            builder.exerciseId(customExercise.getId()).exerciseName(customExercise.getCustomExName()).exerciseType("무게와 횟수").exerciseTarget(customExercise.getExerciseTarget()).exerciseEquipment(customExercise.getExerciseEquipment().getKoreanName()).gifUrl(customExercise.getCustomGifUrl());
        }
        return builder.build();
    }

}
