package team9499.commitbody.domain.exercise.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum ExerciseTarget {

    복근, 등, 가슴, 엉덩이, 대퇴사두근, 삼두, 이두, 어깨, 종아리, 햄스트링, 전완, 기타;

    @JsonCreator
    public static ExerciseTarget fromEventStatus(String val) {
        if (val ==null || val.equals("")){
            return null;
        }
        return Arrays.stream(values())
                .findFirst()
                .orElse(null);
    }
}
