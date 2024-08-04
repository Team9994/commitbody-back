package team9499.commitbody.global.Exception;

public class SecurityException extends RuntimeException {

    private final ExceptionStatus exceptionStatus;
    private final ExceptionType exceptionType;

    public SecurityException(ExceptionStatus exceptionStatus,ExceptionType exceptionType) {
        super(exceptionType.getMessage());
        this.exceptionStatus = exceptionStatus;
        this.exceptionType = exceptionType;
    }

    public int getExceptionStatus() {
        return exceptionStatus.getStatus();
    }
}
