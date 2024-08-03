package team9499.commitbody.global.Exception;

public class JwtTokenException extends RuntimeException{

    private final ExceptionType exceptionType;

    public JwtTokenException(ExceptionType exceptionType) {
        super(exceptionType.getMessage());
        this.exceptionType = exceptionType;
    }
}
