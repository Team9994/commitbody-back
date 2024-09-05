package team9499.commitbody.domain.article.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum ArticleType {

    EXERCISE("운동 인증"),
    INFO_QUESTION("정보 질문");

    private final String description;

    ArticleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static ArticleType fromKorean(String value){
        for (ArticleType articleType : ArticleType.values()) {
            if (articleType.getDescription().equals(value)){
                return articleType;
            }
        }
        return null;
    }

    @JsonCreator
    public static ArticleType fromEventStatus(String val) {
        if (val ==null || val.equals("")){
            return null;
        }
        return Arrays.stream(values())
                .filter(articleType -> articleType.name().equalsIgnoreCase(val))
                .findFirst()
                .orElse(null);
    }
}
