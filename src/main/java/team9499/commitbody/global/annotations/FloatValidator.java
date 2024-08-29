package team9499.commitbody.global.annotations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FloatValidator implements ConstraintValidator<ValidFloat,Float> {

    private ValidFloat validFloat;
    private String requestValue;

    @Override
    public void initialize(ValidFloat constraintAnnotation) {
        this.validFloat = constraintAnnotation;
        this.requestValue = constraintAnnotation.value();
    }
    @Override
    public boolean isValid(Float value, ConstraintValidatorContext context) {
        boolean status = true;
        if (requestValue.equals("weight")){     // 몸무게 검증(10 ~ 199)
            if (value<=10.0 || value>200.0){
                status =  false;
            }
        }else if (requestValue.equals("height")){       // 키 검증 (80 ~ 300)
            if (value<=80.0 || value>300.0){
                status =  false;
            }
        }else {
            if (value<=0.0 || value>100.0){     // 골격근량, 체지방량 검증(0~100)
                status =  false;
            }
        }

        return status;
    }
}
