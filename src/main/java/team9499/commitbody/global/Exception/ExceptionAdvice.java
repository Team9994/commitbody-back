package team9499.commitbody.global.Exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import team9499.commitbody.global.payload.ErrorResponse;

@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> serverException(ServerException e){
        ErrorResponse er = new ErrorResponse<>(false, e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(500).body(er);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> noSuchException(NoSuchException e){
        ErrorResponse er = new ErrorResponse(false, e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(400).body(er);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> jwtTokenException(JwtTokenException e){
        ErrorResponse er = new ErrorResponse(false, e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(401).body(er);
    }
}
