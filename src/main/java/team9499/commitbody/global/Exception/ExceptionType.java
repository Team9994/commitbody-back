package team9499.commitbody.global.Exception;

public enum ExceptionType {

    SERVER_ERROR("서버 오류가 발생했습니다."),
    NO_SUCH_DATA("해당 정보를 찾을수 없습니다."),
    No_SUCH_MEMBER("사용자를 찾을수 없습니다."),
    TOKEN_NOT_FOUND("토큰이 존재하지 않습니다."),
    INVALID_TOKEN_MESSAGE("사용할 수 없는 토큰입니다."),
    TOKEN_EXPIRED("만료된 토큰 입니다."),
    LOGIN_REQUIRED("접근하려는 페이지에 접근하려면 로그인해야 합니다."),
    ACCESS_DENIED("이 리소스에 접근할 권한이 없습니다."),
    DUPLICATE_NICKNAME("중복된 닉네임 입니다.");


    private final String message;

    ExceptionType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
