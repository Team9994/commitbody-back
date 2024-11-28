package team9499.commitbody.domain.exercise.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.exercise.domain.CustomExercise;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.ExerciseInterest;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.routin.domain.RoutineDetails;
import team9499.commitbody.global.constants.ElasticFiled;

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

    public static CustomExerciseDto of(RoutineDetails routineDetails, CustomExercise customExercise){
        return CustomExerciseDto.builder()
                .routineDetailId(routineDetails.getId())
                .exerciseId(customExercise.getId())
                .source(ElasticFiled.CUSTOM)
                .exerciseName(customExercise.getCustomExName())
                .gifUrl(customExercise.getCustomGifUrl())
                .sets(routineDetails.getTotalSets())
                .orders(routineDetails.getOrders())
                .exerciseType("무게와 횟수").build();

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
