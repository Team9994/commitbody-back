package team9499.commitbody.domain.routin.dto.rqeust;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team9499.commitbody.domain.exercise.dto.ExerciseDto;
import team9499.commitbody.domain.routin.dto.RoutineSetsDto;

import java.util.List;

@Data
public class UpdateRoutineRequest {

    private String updateRoutineName;

    private List<Long> deleteRoutines;

    private List<UpdateSets> updateSets;

    private List<DeleteSets> deleteSets;

    private List<ExerciseDto> newExercises;

    private List<ChangeExercise> changeExercises;

    private List<ChangeOrders> changeOrders;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateSets {
        private Long routineDetailsId;
        private List<RoutineSetsDto> newSets;
        private List<RoutineSetsDto> updateSets;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeleteSets {
        private Long routineDetailsId;
        private List<Long> setsIds;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewRoutines {
        private String source;
        private List<ExerciseDto> newExercises;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewExercise {
        private Long exerciseId;
        private List<RoutineSetsDto> sets;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChangeExercise{
        private Long routineDetailsId;
        private Long exerciseId;
        private String source;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChangeOrders{
        private Long routineDetailsId;
        private Integer orders;
    }


}