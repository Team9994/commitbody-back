package team9499.commitbody.global.Exception;

public class InvalidUsageException extends RuntimeException{

    private final ExceptionStatus exceptionStatus;
    private final ExceptionType exceptionType;


    public InvalidUsageException(ExceptionStatus exceptionStatus,ExceptionType exceptionType) {
        super(exceptionType.getMessage());
        this.exceptionStatus = exceptionStatus;
        this.exceptionType = exceptionType;
    }

    public int getExceptionStatus() {
        return exceptionStatus.getStatus();
    }
}
