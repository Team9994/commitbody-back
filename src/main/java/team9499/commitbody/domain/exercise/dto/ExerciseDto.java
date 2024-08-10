package team9499.commitbody.domain.exercise.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExerciseDto {

    private Long exerciseId;

    private String exerciseName;

    private String gifUrl;

    private ExerciseTarget exerciseTarget;

    private String exerciseType;

    private ExerciseEquipment exerciseEquipment;

    private Integer sets;

    public static ExerciseDto of(Long exerciseId, String exerciseName, String gifUrl,Integer sets,String exerciseType){
        return ExerciseDto.builder().exerciseId(exerciseId).exerciseName(exerciseName).gifUrl(gifUrl).sets(sets).exerciseType(exerciseType).build();
    }
}
