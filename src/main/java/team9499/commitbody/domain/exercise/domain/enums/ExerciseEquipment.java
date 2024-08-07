package team9499.commitbody.domain.exercise.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

/*
운동 장비
 */
public enum ExerciseEquipment {

    BODYWEIGHT("맨몸"),
    CABLE("케이블"),
    MACHINE("머신"),
    WEIGHT("중량"),
    BARBELL("바벨"),
    STRETCHING("스트레칭"),
    DUMBBELL("덤벨"),
    SMITH_MACHINE("스미스 머신"),
    BAND("밴드"),
    CARDIO("유산소");

    private final String koreanName;

    ExerciseEquipment(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {
        return koreanName;
    }
    public static ExerciseEquipment fromKoreanName(String koreanName) {
        for (ExerciseEquipment equipment : ExerciseEquipment.values()) {
            if (equipment.getKoreanName().equals(koreanName)) {
                return equipment;
            }
        }
        return null;
    }

    @JsonCreator
    public static ExerciseEquipment fromEventStatus(String val) {
        if (val ==null || val.equals("")){
            return null;
        }
        return Arrays.stream(values())
                .findFirst()
                .orElse(null);
    }
}
