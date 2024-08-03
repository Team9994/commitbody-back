package team9499.commitbody.global.Exception;

public class ServerException extends RuntimeException{

    private final ExceptionType exceptionType;
    private final ExceptionStatus exceptionStatus;
    public ServerException(ExceptionStatus exceptionStatus,ExceptionType exceptionType) {
        super(exceptionType.getMessage());
        this.exceptionType = exceptionType;
        this.exceptionStatus=exceptionStatus;
    }

    public int getExceptionStatus() {
        return exceptionStatus.getStatus();
    }
}
