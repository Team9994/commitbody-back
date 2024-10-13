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
    DUPLICATE_NICKNAME("중복된 닉네임 입니다."),
    INVALID_FILE_FORMAT("올바른 파일 형식이 아닙니다."),
    ONLY_IMAGE("이미지 파일만 등록 가능합니다."),
    ALREADY_REQUESTED("이미 처리된 요청입니다."),
    AUTHOR_ONLY("작성자만 이용할 수 있습니다."),
    BLOCK("사용자를 차단한 상태입니다."),
    PRIVATE_ACCOUNT("비공개 계정입니다."),
    NOT_USE_ZERO("0 이상인 값을 입력해주세요.");


    private final String message;

    ExceptionType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
