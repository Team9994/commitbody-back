package team9499.commitbody.global.Exception;

public enum ExceptionStatus {

    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    SERVER_ERROR(500);

    private final int status;

    ExceptionStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
