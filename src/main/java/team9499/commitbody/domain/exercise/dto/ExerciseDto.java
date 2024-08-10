package team9499.commitbody.domain.exercise.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.routin.dto.RoutineSetsDto;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExerciseDto {

    private Long routineDetailId;

    private Long exerciseId;

    private String exerciseName;

    private String gifUrl;

    private ExerciseTarget exerciseTarget;

    private String exerciseType;

    private ExerciseEquipment exerciseEquipment;

    private Integer sets;

    private List<RoutineSetsDto> routineSets;

    public static ExerciseDto of(Long routineDetailId, Long exerciseId, String exerciseName, String gifUrl,Integer sets,String exerciseType,List<RoutineSetsDto> routineSets) {
        return ExerciseDto.builder().routineDetailId(routineDetailId).exerciseId(exerciseId).exerciseName(exerciseName).gifUrl(gifUrl).sets(sets).exerciseType(exerciseType).routineSets(routineSets).build();
    }
}
