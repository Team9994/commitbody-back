package team9499.commitbody.domain.article.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum ArticleCategory {

    INFORMATION("정보"),
    FEEDBACK("피드백"),
    BODY_REVIEW("몸평");

    private final String description;

    ArticleCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static ArticleCategory fromKorean(String value){
        for (ArticleCategory articleCategory : ArticleCategory.values()) {
            if (articleCategory.getDescription().equals(value)){
                return articleCategory;
            }
        }
        return null;
    }

    @JsonCreator
    public static ArticleCategory fromEventStatus(String val) {
        if (val ==null || val.equals("")){
            return null;
        }
        return Arrays.stream(values())
                .filter(articleCategory -> articleCategory.name().equalsIgnoreCase(val))
                .findFirst()
                .orElse(null);
    }
}
