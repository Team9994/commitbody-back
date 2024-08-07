package team9499.commitbody.global.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class Pointcuts {

    @Pointcut("execution(* *..*Controller.*(..))")
    void executionTime(){}

    @Pointcut("execution(* *..*Controller.*(..))")
    void validResponse(){}

}
