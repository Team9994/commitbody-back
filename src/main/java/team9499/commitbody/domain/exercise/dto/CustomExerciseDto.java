package team9499.commitbody.domain.exercise.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import team9499.commitbody.domain.exercise.domain.ExerciseInterest;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomExerciseDto {

    private Long customExerciseId;

    private String exerciseName;

    private String gifUrl;

    private ExerciseEquipment exerciseEquipment;

    private ExerciseInterest exerciseInterest;

    private Integer sets;

    public static CustomExerciseDto of(Long customExerciseId,String exerciseName,String gifUrl,Integer sets){
        return CustomExerciseDto.builder().customExerciseId(customExerciseId).exerciseName(exerciseName).gifUrl(gifUrl).sets(sets).build();
    }

}
