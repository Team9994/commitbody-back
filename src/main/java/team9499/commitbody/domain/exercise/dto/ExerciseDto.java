package team9499.commitbody.domain.exercise.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.routin.dto.RoutineSetsDto;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class ExerciseDto {

    private Long routineDetailId;

    private Long exerciseId;

    private String exerciseName;

    private String gifUrl;

    private ExerciseTarget exerciseTarget;

    private String exerciseType;

    private ExerciseEquipment exerciseEquipment;

    private Integer sets;

    private Integer orders; // 운동 순서

    private List<RoutineSetsDto> routineSets;

    private String source;

    public static ExerciseDto of(Long routineDetailId, Long exerciseId, String exerciseName, String gifUrl,Integer sets,String exerciseType,Integer orders,List<RoutineSetsDto> routineSets) {
        return ExerciseDto.builder().routineDetailId(routineDetailId).exerciseId(exerciseId).exerciseName(exerciseName).gifUrl(gifUrl).sets(sets).exerciseType(exerciseType).orders(orders).routineSets(routineSets).build();
    }
}
