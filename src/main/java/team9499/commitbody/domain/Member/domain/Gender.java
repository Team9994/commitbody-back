package team9499.commitbody.domain.Member.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum Gender {

    MALE("남성"),
    FEMALE("여성");

    private final String genderKR;

    Gender(String genderKR) {
        this.genderKR = genderKR;
    }
    public String getGenderKR(){
        return genderKR;
    }

    public static Gender fromGenderKR(String genderKR) {
        for (Gender gender : values()) {
            if (gender.getGenderKR().equals(genderKR)) {
                return gender;
            }
        }
        return null;
    }

    @JsonCreator
    public static Gender fromEventStatus(String val) {
        if (val ==null || val.equals("")){
            return null;
        }
        return Arrays.stream(values())
                .findFirst()
                .orElse(null);
    }
}
