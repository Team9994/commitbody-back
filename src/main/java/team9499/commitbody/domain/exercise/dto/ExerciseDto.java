package team9499.commitbody.domain.exercise.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.exercise.domain.Exercise;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseTarget;
import team9499.commitbody.domain.record.dto.RecordSetsDto;
import team9499.commitbody.domain.routin.domain.RoutineDetails;
import team9499.commitbody.global.constants.ElasticFiled;

import java.util.List;

import static team9499.commitbody.global.constants.ElasticFiled.*;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class ExerciseDto {

    private Long routineDetailId;

    private Long exerciseId;

    private String source;

    private String exerciseName;

    private String gifUrl;

    private ExerciseTarget exerciseTarget;

    private String exerciseType;

    private ExerciseEquipment exerciseEquipment;

    private Integer sets;

    private Integer orders; // 운동 순서

    private List<RecordSetsDto> routineSets;

    private Boolean interest;



    public static ExerciseDto of(Long routineDetailId, Long exerciseId,String exerciseName, String gifUrl,Integer sets,String exerciseType,Integer orders,List<RecordSetsDto> routineSets) {
        return ExerciseDto.builder().routineDetailId(routineDetailId).source("default").exerciseId(exerciseId).exerciseName(exerciseName).gifUrl(gifUrl).sets(sets).exerciseType(exerciseType).orders(orders).routineSets(routineSets).build();
    }
    public static ExerciseDto of(Long routineDetailId, Long exerciseId,String exerciseName, String gifUrl,Integer sets,String exerciseType,Integer orders) {
        return ExerciseDto.builder().routineDetailId(routineDetailId).source("default").exerciseId(exerciseId).exerciseName(exerciseName).gifUrl(gifUrl).sets(sets).exerciseType(exerciseType).orders(orders).build();
    }

    public static ExerciseDto of(Long exerciseId, String name, String gifUrl, String target, String type, String equipment, String source, boolean interest){
        return ExerciseDto.builder().exerciseId(exerciseId).exerciseName(name).gifUrl(gifUrl).exerciseTarget(ExerciseTarget.valueOf(target)).exerciseType(type).exerciseEquipment(ExerciseEquipment.fromEventStatus(equipment))
                .source(source).interest(interest).build();
    }

    public static ExerciseDto of(RoutineDetails routineDetails, Exercise exercise){
        return ExerciseDto.builder().exerciseId(routineDetails.getId())
                .exerciseName(exercise.getExerciseName())
                .gifUrl(exercise.getGifUrl())
                .sets(routineDetails.getTotalSets())
                .exerciseType(exercise.getExerciseType().getDescription())
                .orders(routineDetails.getOrders())
                .source(DEFAULT)
                .build();
    }
}
