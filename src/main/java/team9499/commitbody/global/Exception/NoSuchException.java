package team9499.commitbody.global.Exception;

public class NoSuchException extends RuntimeException{

    private final ExceptionType exceptionType;
    private final ExceptionStatus exceptionStatus;

    public NoSuchException(ExceptionStatus exceptionStatus, ExceptionType exceptionType) {
        super(exceptionType.getMessage());
        this.exceptionType = exceptionType;
        this.exceptionStatus = exceptionStatus;
    }

    public int getExceptionStatus() {
        return exceptionStatus.getStatus();
    }
}
