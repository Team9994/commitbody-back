package team9499.commitbody.domain.exercise.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import team9499.commitbody.domain.exercise.domain.enums.ExerciseType;

@Converter(autoApply = true)
public class ExerciseTypeConverter implements AttributeConverter<ExerciseType,String> {
    @Override
    public String convertToDatabaseColumn(ExerciseType attribute) {
        return attribute == null ? null : attribute.getDescription();
    }
    @Override
    public ExerciseType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ExerciseType.fromExerciseType(dbData);
    }
}
