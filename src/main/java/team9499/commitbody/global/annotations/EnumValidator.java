package team9499.commitbody.global;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnumValidator implements ConstraintValidator<ValidEnum,Enum> {

    private ValidEnum validEnum;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
       this.validEnum = constraintAnnotation;
    }

    @Override
    public boolean isValid(Enum value, ConstraintValidatorContext context) {
        Object[] enumValues = this.validEnum.enumClass().getEnumConstants();
        if (enumValues!=null){
            for (Object enumVale : enumValues){
                if (value == enumVale){
                    return true;
                }
            }
        }
        return false;
    }
}
