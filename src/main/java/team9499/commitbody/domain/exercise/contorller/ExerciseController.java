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
import team9499.commitbody.domain.exercise.dto.CustomUpdateExerciseReqeust;
import team9499.commitbody.domain.exercise.dto.InterestExerciseRequest;
import team9499.commitbody.domain.exercise.dto.SearchExerciseResponse;
import team9499.commitbody.domain.exercise.dto.response.ExerciseResponse;
import team9499.commitbody.domain.exercise.event.ElasticDeleteExerciseEvent;
import team9499.commitbody.domain.exercise.event.ElasticExerciseInterest;
import team9499.commitbody.domain.exercise.event.ElasticSaveExerciseEvent;
import team9499.commitbody.domain.exercise.event.ElasticUpdateExerciseEvent;
import team9499.commitbody.domain.exercise.service.ExerciseInterestService;
import team9499.commitbody.domain.exercise.service.ExerciseService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final ExerciseInterestService exerciseInterestService;
    private final ApplicationEventPublisher eventPublisher;

    @Tag(name = "운동", description = "운동 관련 API")
    @Operation(summary = "운동 검색", description = "운동을 검색합니다. [name: 운동명, target: 부위, equipment: 운동 장비, favorite: 관심운동 여부(true/false), from: 시작 위치, size: 페이지당 표시 데이터 양]",tags = "운동")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "EX) - /api/v1/search-exercise?name=인클라인&target=가슴&equipment=맨몸&favorite=false&from=0&size=1", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\": true, \"message\": \"성공\", \"data\": {\"totalCount\": 5, \"exercise\": [{\"exerciseId\": \"492\", \"exerciseName\": \"인클라인 푸시업 뎁스 점프\", \"gifUrl\": \"https://EXAMPLE.COM\", \"exerciseTarget\": \"가슴\", \"exerciseType\": \"횟수\", \"exerciseEquipment\": \"맨몸\", \"source\": \"default\", \"interest\": false}]}}")))
    })
    @GetMapping("/search-exercise")
    public ResponseEntity<?> searchExercise(@RequestParam(value = "name",required = false) String name,
                                            @RequestParam(value = "target",required = false) String target,
                                            @RequestParam(value = "equipment",required = false)String equipment,
                                            @RequestParam(value = "interest",required = false) Boolean interest,
                                            @RequestParam(value = "source",required = false)String source,
                                            @RequestParam(value = "from",required = false)Integer from,
                                            @RequestParam(value = "size",required = false)Integer size,
                                            @AuthenticationPrincipal PrincipalDetails principalDetails){
        String memberId = String.valueOf(principalDetails.getMember().getId());
        SearchExerciseResponse searchExerciseResponse = exerciseService.searchExercise(name, target, equipment,from, size, interest, memberId,source);

        return ResponseEntity.ok(new SuccessResponse<>(true,"성공",searchExerciseResponse));
    }


    @Operation(summary = "커스텀 운동 등록", description = "사용자는 커스텀 운동을 등록가능하며 단일 사진만 등록 가능합니다.",tags = "운동")
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
    @PostMapping(value = "/save-exercise", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveExercise(@Valid @RequestPart(name = "customExerciseReqeust") CustomExerciseReqeust customExerciseReqeust, BindingResult result,
                                          @RequestPart(name ="file" , required = false) MultipartFile file,
                                          @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long id = principalDetails.getMember().getId();
        Long customExerciseId = exerciseService.saveCustomExercise(customExerciseReqeust.getExerciseName(), customExerciseReqeust.getExerciseTarget(),
                customExerciseReqeust.getExerciseEquipment(), id, file);

        eventPublisher.publishEvent(new ElasticSaveExerciseEvent(customExerciseId));

        return ResponseEntity.ok(new SuccessResponse<>(true,"저장 성공"));

    }

    @Operation(summary = "커스텀 운동 수정", description = "사용자는 커스텀 운동을 수정가능합니다.",tags = "운동")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"업데이트 성공\"}"))),
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
    @PostMapping(value = "/update-exercise", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateExercise(@Valid @RequestPart(name = "customUpdateExerciseReqeust") CustomUpdateExerciseReqeust customUpdateExerciseReqeust, BindingResult result,
                                            @RequestPart(name ="file" , required = false) MultipartFile file,
                                            @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long id = principalDetails.getMember().getId();
        Long customExerciseId = exerciseService.updateCustomExercise(customUpdateExerciseReqeust.getExerciseName(), customUpdateExerciseReqeust.getExerciseTarget(),
                customUpdateExerciseReqeust.getExerciseEquipment(), id, customUpdateExerciseReqeust.getCustomExerciseId(),file);
        eventPublisher.publishEvent(new ElasticUpdateExerciseEvent(customExerciseId,customUpdateExerciseReqeust.getSource()));

        return ResponseEntity.ok(new SuccessResponse<>(true,"업데이트 성공"));

    }

    @Operation(summary = "커스텀 운동 삭제", description = "등록한 사용자만 커스텀 운동을 삭제 가능합니다.",tags = "운동")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"삭제 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 커스텀 운동 미존재", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"해당 정보를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @DeleteMapping( "/delete-exercise")
    public ResponseEntity<?> deleteExercise(@RequestParam("id") Long customExerciseId,
                                            @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();
        exerciseService.deleteCustomExercise(customExerciseId,memberId);
        eventPublisher.publishEvent(new ElasticDeleteExerciseEvent(customExerciseId));

        return ResponseEntity.ok(new SuccessResponse<>(true,"삭제 성공"));
    }

    @Operation(summary = "관심운동", description = "운동에 관심운동을 등록 해제 합니다. source :[ 기본 제공 운동 : default_, 커스텀 운동 : custom_] 사용합니다.",tags = "운동")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200-1", description = "관심운동 등록", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"등록\"}"))),
            @ApiResponse(responseCode = "200-2", description = "관심운동 해제", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"해제\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @PostMapping("/interest-exercise")
    public ResponseEntity<?> interestExercise(@RequestBody InterestExerciseRequest interestExerciseRequest,
                                              @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();
        String interestStatus = exerciseInterestService.updateInterestStatus(interestExerciseRequest.getExerciseId(), memberId, interestExerciseRequest.getSource());
        eventPublisher.publishEvent(new ElasticExerciseInterest(interestExerciseRequest.getExerciseId(),interestExerciseRequest.getSource(),interestStatus,memberId));
        return ResponseEntity.ok(new SuccessResponse<>(true,interestStatus));
    }


    @Tag(name = "운동 상세", description = "운동 상세페이지 관련 API")
    @Operation(summary = "운동 상세조회 - 통계", description = "운동의 해당 운동의 대한 통계및 운동의 대한 순서를 조회할수 있습니다.",tags = "운동 상세")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"조회 성공\",\"data\":{\"exerciseId\":1,\"exerciseName\":\"3/4 싯업\",\"exerciseTarget\":\"복근\",\"interestStatus\":false,\"exerciseEquipment\":\"맨몸\",\"exerciseType\":\"횟수\",\"gifUrl\":\"https://v2.exercisedb.io/image/oAVJS-wlSfNhXd\",\"totalValue\":90,\"maxValue\":5,\"weekValue\":72,\"calculateRankPercentage\":0,\"day\":{\"MONDAY\":9,\"THURSDAY\":9,\"SUNDAY\":9,\"TUESDAY\":9},\"exerciseMethods\":[\"등을 대고 눕고 무릎을 구부리며 발은 바닥에 평평하게 붙입니다.\"],\"records\":[{\"date\":\"2024-08-20T08:46:33.368\",\"sets\":[{\"reps\":4},{\"reps\":5}]}]}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 정보 미존재 시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"해당 정보를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/exercise/{id}")
    public ResponseEntity<?> getDetailExercise(@PathVariable("id") Long id, @RequestParam("source")String source,
                                  @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();
        ExerciseResponse exerciseResponse = exerciseService.detailsExercise(id,memberId,source);
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",exerciseResponse));
    }
}
