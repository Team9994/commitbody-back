package team9499.commitbody.domain.exercise.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseEquipment;

@Converter(autoApply = true)
public class ExerciseEquipmentConverter implements AttributeConverter<ExerciseEquipment, String> {

    @Override
    public String convertToDatabaseColumn(ExerciseEquipment attribute) {
        return attribute == null ? null : attribute.getKoreanName();
    }

    @Override
    public ExerciseEquipment convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ExerciseEquipment.fromKoreanName(dbData);
    }
}
