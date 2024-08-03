package team9499.commitbody.global.Exception;

public enum ExceptionType {

    SERVER_ERROR("서버 오류가 발생했습니다."),
    NO_SUCH_DATA("해당 정보를 찾을수 없습니다."),
    No_SUCH_MEMBER("사용자를 찾을수 없습니다."),
    TOKEN_NOT_FOUND("토큰이 존재하지 않습니다."),
    INVALID_TOKEN_MESSAGE("사용할 수 없는 토큰입니다.");


    private final String message;

    ExceptionType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
