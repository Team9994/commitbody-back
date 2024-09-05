package team9499.commitbody.global.Exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import team9499.commitbody.global.payload.ErrorResponse;

@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> serverException(ServerException e){
        ErrorResponse er = new ErrorResponse<>(false, e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(e.getExceptionStatus()).body(er);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> noSuchException(NoSuchException e){
        ErrorResponse er = new ErrorResponse(false, e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(e.getExceptionStatus()).body(er);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> jwtTokenException(JwtTokenException e){
        ErrorResponse er = new ErrorResponse(false, e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(e.getExceptionStatus()).body(er);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> InvalidUsageException(InvalidUsageException e){
        ErrorResponse er = new ErrorResponse(false, e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(e.getExceptionStatus()).body(er);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> MaxUploadSizeExceededException(MaxUploadSizeExceededException e){
        ErrorResponse er = new ErrorResponse(false,"저장 가능한 용량을 초과 했습니다.");
        return ResponseEntity.status(400).body(er);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> BlockMemberException(BlockException e){
        ErrorResponse er = new ErrorResponse<>(false, e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(e.getExceptionStatus()).body(er);
    }
}
