package team9499.commitbody.domain.article.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum ArticleCategory {

    information("정보");

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
                .findFirst()
                .orElse(null);
    }
}
