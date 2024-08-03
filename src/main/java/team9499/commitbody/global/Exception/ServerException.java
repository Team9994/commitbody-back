package team9499.commitbody.global.Exception;

public class ServerException extends RuntimeException{

    private final ExceptionType exceptionType;
    public ServerException(ExceptionType exceptionType) {
        super(exceptionType.getMessage());
        this.exceptionType = exceptionType;
    }

}
