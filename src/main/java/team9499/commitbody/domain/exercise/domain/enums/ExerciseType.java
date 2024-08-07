package team9499.commitbody.domain.exercise.domain.enums;

public enum ExerciseType {
    WEIGHT_AND_REPS("무게와 횟수"),
    REPS_ONLY("횟수"),
    TIME_ONLY("시간 단위");

    private final String description;

    ExerciseType(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }

    public static ExerciseType fromExerciseType(String type) {
        for (ExerciseType exerciseType : values()) {
            if (exerciseType.getDescription().equals(type)) {
                return exerciseType;
            }
        }
        return null;
    }

}
