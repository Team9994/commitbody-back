package team9499.commitbody.domain.article.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public enum ArticleCategory {

    ALL("전체"),
    FOLLOWING("팔로잉"),
    POPULAR("인기"),
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
            log.info("value={}",value.equals(articleCategory.getDescription()));
            if (articleCategory.getDescription().equals(value)){
                return articleCategory;
            }
        }
        return null;
    }

    public static ArticleCategory stringToEnum(String value){
        return ArticleCategory.valueOf(value);
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
