package team9499.commitbody.domain.exercise.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.ExerciseInterest;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomExerciseDto {

    private Long routineDetailId;

    private Long exerciseId;

    private Long memberId;

    private String source;

    private String exerciseName;

    private String gifUrl;

    private ExerciseEquipment exerciseEquipment;

    private ExerciseInterest exerciseInterest;

    private ExerciseTarget exerciseTarget;

    private String exerciseType;

    private Integer sets;

    private Integer orders; // 운동 순서


    public static CustomExerciseDto of(Long routineDetailId, Long customExerciseId,String exerciseName,String gifUrl,Integer sets,String exerciseType,Integer orders){
        return CustomExerciseDto.builder().routineDetailId(routineDetailId).exerciseId(customExerciseId).source("custom").exerciseName(exerciseName).gifUrl(gifUrl).sets(sets).orders(orders).exerciseType(exerciseType).build();
    }


    public static CustomExerciseDto fromDto(CustomExercise customExercise,String imgUrl){
        return CustomExerciseDto.builder()
                .exerciseId(customExercise.getId())
                .exerciseName(customExercise.getCustomExName())
                .gifUrl(imgUrl)
                .exerciseTarget(customExercise.getExerciseTarget())
                .exerciseEquipment(customExercise.getExerciseEquipment())
                .memberId(customExercise.getMember().getId())
                .build();

    }

}
