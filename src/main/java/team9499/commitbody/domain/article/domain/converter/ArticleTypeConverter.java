package team9499.commitbody.domain.article.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import team9499.commitbody.domain.article.domain.ArticleType;

@Converter(autoApply = true)
public class ArticleTypeConverter implements AttributeConverter<ArticleType,String> {
    @Override
    public String convertToDatabaseColumn(ArticleType attribute) {
        return attribute == null ? null : attribute.getDescription();
    }

    @Override
    public ArticleType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ArticleType.fromKorean(dbData);
    }
}
