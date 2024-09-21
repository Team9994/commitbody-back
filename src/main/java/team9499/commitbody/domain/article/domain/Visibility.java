package team9499.commitbody.domain.article.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum Visibility {

    PUBLIC("전체 공개"),
    FOLLOWERS_ONLY("팔로워만 공개"),
    PRIVATE("비공개");

    private final String description;

    public static Visibility fromKorean(String value){
        for (Visibility visibility : Visibility.values()) {
            if (visibility.getDescription().equals(value)){
                return visibility;
            }
        }
        return null;
    }

    @JsonCreator
    public static Visibility fromEventStatus(String val) {
        if (val ==null || val.equals("")){
            return null;
        }
        return Arrays.stream(values())
                .filter(visibility -> visibility.name().equalsIgnoreCase(val))
                .findFirst()
                .orElse(null);
    }
}
