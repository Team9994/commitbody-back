package team9499.commitbody.domain.Member.domain;

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
}
