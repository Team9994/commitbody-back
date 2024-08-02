package team9499.commitbody.global.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ErrorResponse<T> {

    private boolean success;        // success : false

    private String message;         // message : 실패

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;                 // data : { id : 1l }

    public ErrorResponse(boolean success, String message) {       // 추가적인 정보가 필요없는 생성자
        this.success = success;
        this.message = message;
    }

    public ErrorResponse(boolean success, String message, T data) {       // 추가적인 정보가 필요로 하는 생성자
        this.success = success;
        this.message = message;
        this.data = data;
    }
}
