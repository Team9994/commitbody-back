package team9499.commitbody.global.payload;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ErrorResponse<T> {

    private boolean success;        // success : false

    private T message;         // message : 실패

    public ErrorResponse(boolean success, T message) {       // 추가적인 정보가 필요없는 생성자
        this.success = success;
        this.message = message;
    }
}
