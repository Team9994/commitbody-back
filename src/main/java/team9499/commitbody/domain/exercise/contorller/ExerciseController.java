package team9499.commitbody.domain.exercise.contorller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.exercise.dto.CustomExerciseReqeust;
import team9499.commitbody.domain.exercise.dto.SearchExerciseResponse;
import team9499.commitbody.domain.exercise.event.ElasticSaveExerciseEvent;
import team9499.commitbody.domain.exercise.service.ExerciseService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;

@RestController
@Tag(name = "운동",description = "운동관련 API")
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final ApplicationEventPublisher eventPublisher;

    @Operation(summary = "운동 검색", description = "운동을 검색합니다. [name: 운동명, target: 부위, equipment: 운동 장비, favorite: 관심운동 여부(true/false), from: 시작 위치, size: 페이지당 표시 데이터 양]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "EX) - /api/v1/search-exercise?name=인클라인&target=가슴&equipment=맨몸&favorite=false&from=0&size=1", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"성공\", \"data\": {\"totalCount\": 5, \"exercise\": [{\"exerciseId\": \"492\", \"exerciseName\": \"인클라인 푸시업 뎁스 점프\", \"gifUrl\": \"https://EXAMPLE.COM\", \"exerciseTarget\": \"가슴\", \"exerciseType\": \"횟수\", \"exerciseEquipment\": \"맨몸\", \"source\": \"default\", \"favorites\": false}]}}")))
    })
    @GetMapping("/search-exercise")
    public ResponseEntity<?> searchExercise(@RequestParam(value = "name",required = false) String name,
                                            @RequestParam(value = "target",required = false) String target,
                                            @RequestParam(value = "equipment",required = false)String equipment,
                                            @RequestParam(value = "favorite",required = false) Boolean favorite,
                                            @RequestParam(value = "from",required = false)Integer from,
                                            @RequestParam(value = "size",required = false)Integer size,
                                            @AuthenticationPrincipal PrincipalDetails principalDetails){
        String memberId = String.valueOf(principalDetails.getMember().getId());
        SearchExerciseResponse searchExerciseResponse = exerciseService.searchExercise(name, target, equipment,from, size, favorite, memberId);

        return ResponseEntity.ok(new SuccessResponse<>(true,"성공",searchExerciseResponse));
    }


    @Operation(summary = "커스텀 운동 등록", description = "사용자는 커스텀 운동을 등록가능하며 단일 사진만 등록 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"저장 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 값 입렵 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"실패\",\"data\":{\"필드명\": \"오류 내용\"}}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 파일 용량 초과(5MB 이하만 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"저장 가능한 용량을 초과 했습니다.\"}"))),
            @ApiResponse(responseCode = "400_4", description = "BADREQUEST - 불가능한 이미지 파일 저장시(jpeg, jpg, png, gif 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"올바른 이미지 형식이 아닙니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveExercise(@Valid @RequestPart(name = "customExerciseReqeust", required = false) CustomExerciseReqeust customExerciseReqeust, BindingResult result,
                                          @RequestPart(name ="file" , required = false) MultipartFile file,
                                          @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long id = principalDetails.getMember().getId();
        Long customExerciseId = exerciseService.saveCustomExercise(customExerciseReqeust.getExerciseName(), customExerciseReqeust.getExerciseTarget(),
                customExerciseReqeust.getExerciseEquipment(), id, file);

        eventPublisher.publishEvent(new ElasticSaveExerciseEvent(customExerciseId));

        return ResponseEntity.ok(new SuccessResponse<>(true,"저장 성공"));

    }
}
