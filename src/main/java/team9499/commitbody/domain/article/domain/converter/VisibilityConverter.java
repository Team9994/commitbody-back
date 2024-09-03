package team9499.commitbody.domain.article.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import team9499.commitbody.domain.article.domain.Visibility;

@Converter(autoApply = true)
public class VisibilityConverter implements AttributeConverter<Visibility,String> {

    @Override
    public String convertToDatabaseColumn(Visibility attribute) {
        return attribute == null ? null : attribute.getDescription();
    }

    @Override
    public Visibility convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Visibility.fromKorean(dbData);
    }
}
