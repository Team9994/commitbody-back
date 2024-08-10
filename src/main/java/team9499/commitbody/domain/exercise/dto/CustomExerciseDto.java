package team9499.commitbody.domain.exercise.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import team9499.commitbody.domain.exercise.domain.ExerciseInterest;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.routin.dto.RoutineSetsDto;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomExerciseDto {

    private Long routineDetailId;

    private Long customExerciseId;

    private String exerciseName;

    private String gifUrl;

    private ExerciseEquipment exerciseEquipment;

    private ExerciseInterest exerciseInterest;

    private String exerciseType;

    private Integer sets;

    private List<RoutineSetsDto> routineSets;

    public static CustomExerciseDto of(Long routineDetailId, Long customExerciseId,String exerciseName,String gifUrl,Integer sets,String exerciseType,List<RoutineSetsDto> routineSetsDtos){
        return CustomExerciseDto.builder().routineDetailId(routineDetailId).customExerciseId(customExerciseId).exerciseName(exerciseName).gifUrl(gifUrl).sets(sets).exerciseType(exerciseType).routineSets(routineSetsDtos).build();
    }

}
