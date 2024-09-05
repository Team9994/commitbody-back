package team9499.commitbody.domain.article.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import team9499.commitbody.domain.article.domain.ArticleCategory;

@Converter(autoApply = true)
public class ArticleCategoryConverter implements AttributeConverter<ArticleCategory,String> {

    @Override
    public String convertToDatabaseColumn(ArticleCategory attribute) {
        return attribute==null ? null : attribute.getDescription();
    }

    @Override
    public ArticleCategory convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ArticleCategory.fromKorean(dbData);
    }
}
