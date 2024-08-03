package team9499.commitbody.global.Exception;

public class NoSuchException extends RuntimeException{

    private final ExceptionType exceptionType;

    public NoSuchException(ExceptionType exceptionType) {
        super(exceptionType.getMessage());
        this.exceptionType = exceptionType;
    }
}
