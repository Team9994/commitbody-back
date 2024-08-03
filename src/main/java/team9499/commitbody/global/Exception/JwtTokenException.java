package team9499.commitbody.global.Exception;

public class JwtTokenException extends RuntimeException{

    private final ExceptionType exceptionType;
    private final ExceptionStatus exceptionStatus;

    public JwtTokenException(ExceptionStatus exceptionStatus, ExceptionType exceptionType) {
        super(exceptionType.getMessage());
        this.exceptionStatus = exceptionStatus;
        this.exceptionType = exceptionType;
    }

    public int getExceptionStatus() {
        return exceptionStatus.getStatus();
    }
}
