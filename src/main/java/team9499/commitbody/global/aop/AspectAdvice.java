package team9499.commitbody.global.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import team9499.commitbody.global.payload.ErrorResponse;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Aspect
@Component("aspectAdvice")
public class AspectAdvice {

    @Around("team9499.commitbody.global.aop.Pointcuts.executionTime()")
    public Object executionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toLongString();

        try {
            log.info("start method: {}", methodName);
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - start;
            log.info("end method:{} time in {} ms", methodName, executionTime);

            return result;
        } catch (Throwable e) {
            log.error("Exception in method {}: {}", methodName, e.getMessage());
            throw e;
        }
    }

    @Around("team9499.commitbody.global.aop.Pointcuts.validResponse()")
    public Object validResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        for (Object arg : args){
            if (arg instanceof BindingResult){
                BindingResult result = (BindingResult) arg;
                if (result.hasErrors()){
                    Map<String,String> errorMap = new LinkedHashMap<>();
                    for(FieldError error : result.getFieldErrors()){
                        errorMap.put(error.getField(),error.getDefaultMessage());
                    }
                    return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse<>(false, "실패", errorMap));
                }
            }
        }
        return joinPoint.proceed();
    }
}
