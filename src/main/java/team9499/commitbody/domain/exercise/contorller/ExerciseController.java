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
import team9499.commitbody.domain.exercise.dto.*;
import team9499.commitbody.domain.exercise.dto.response.ExerciseResponse;
import team9499.commitbody.domain.exercise.event.ElasticDeleteExerciseEvent;
import team9499.commitbody.domain.exercise.event.ElasticExerciseInterest;
import team9499.commitbody.domain.exercise.event.ElasticSaveExerciseEvent;
import team9499.commitbody.domain.exercise.event.ElasticUpdateExerciseEvent;
import team9499.commitbody.domain.exercise.service.ElasticExerciseService;
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
    private final ElasticExerciseService elasticExerciseService;
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
        SearchExerciseResponse searchExerciseResponse = elasticExerciseService.searchExercise(name, target, equipment,from, size, interest, memberId,source);

        return ResponseEntity.ok(new SuccessResponse<>(true,"성공",searchExerciseResponse));
    }


    @Operation(summary = "커스텀 운동 등록", description = "사용자는 커스텀 운동을 등록가능하며 단일 사진만 등록 가능합니다.",tags = "운동")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"저장 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 값 입렵 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":{\"필드명\": \"오류 내용\"}}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 파일 용량 초과(5MB 이하만 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"저장 가능한 용량을 초과 했습니다.\"}"))),
            @ApiResponse(responseCode = "400_4", description = "BADREQUEST - 불가능한 파일 형식 저장시(jpeg, jpg, png, gif 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"올바른 파일 형식이 아닙니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PostMapping(value = "/save-exercise", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveExercise(@Valid @RequestPart(name = "customExerciseRequest") CustomExerciseRequest customExerciseRequest, BindingResult result,
                                          @RequestPart(name ="file" , required = false) MultipartFile file,
                                          @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long id = principalDetails.getMember().getId();
        CustomExerciseDto customExerciseDto = exerciseService.saveCustomExercise(customExerciseRequest.getExerciseName(), customExerciseRequest.getExerciseTarget(),
                customExerciseRequest.getExerciseEquipment(), id, file);

        eventPublisher.publishEvent(new ElasticSaveExerciseEvent(customExerciseDto));

        return ResponseEntity.ok(new SuccessResponse<>(true,"저장 성공"));

    }

    @Operation(summary = "커스텀 운동 수정", description = "사용자는 커스텀 운동을 수정가능합니다.",tags = "운동")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"업데이트 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 값 입렵 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":{\"필드명\": \"오류 내용\"}}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 파일 용량 초과(5MB 이하만 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"저장 가능한 용량을 초과 했습니다.\"}"))),
            @ApiResponse(responseCode = "400_4", description = "BADREQUEST - 불가능한 파일 형식 저장시(jpeg, jpg, png, gif 저장가능)",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"올바른 파일 형식이 아닙니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PostMapping(value = "/update-exercise", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateExercise(@Valid @RequestPart(name = "customUpdateExerciseRequest") CustomUpdateExerciseRequest customUpdateExerciseRequest, BindingResult result,
                                            @RequestPart(name ="file" , required = false) MultipartFile file,
                                            @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long id = principalDetails.getMember().getId();
        CustomExerciseDto customExerciseDto = exerciseService.updateCustomExercise(customUpdateExerciseRequest.getExerciseName(), customUpdateExerciseRequest.getExerciseTarget(),
                customUpdateExerciseRequest.getExerciseEquipment(), id, customUpdateExerciseRequest.getCustomExerciseId(), file);
        eventPublisher.publishEvent(new ElasticUpdateExerciseEvent(customExerciseDto,customUpdateExerciseRequest.getSource()));

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
        eventPublisher.publishEvent(new ElasticDeleteExerciseEvent(customExerciseId,memberId));

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
    @Operation(summary = "운동 상세조회 - 통계", description = "운동의 해당 운동의 대한 통계및 운동의 대한 순서를 조회할수 있습니다.(운동의 대한 기록이 없으면 빈값(day,records)으로 넘거갑니다.)",tags = "운동 상세")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"조회 성공\",\"data\":{\"exerciseDto\":{\"exerciseId\":139,\"exerciseName\":\"바이셉스 내로우 풀업\",\"gifUrl\":\"https://v2.exercisedb.io/image/l9pAj7Mhv1me54\",\"exerciseType\":\"횟수\",\"exerciseEquipment\":\"BODYWEIGHT\",\"interest\":false},\"reportDto\":{\"maxRep\":10,\"totalRep\":280,\"weekRep\":60,\"weekReports\":[{\"dayOfWeek\":\"TUESDAY\",\"data\":25}]},\"exerciseMethods\":[\"동작의 정점에서 잠시 멈춘 후| 천천히 원래 위치로 돌아갑니다.\",\"풀업 바에 매달려 손바닥이 자신을 향하게 하고 손이 어깨너비로 벌어지게 합니다.\",\"코어를 조이고 바를 향해 몸을 끌어올리며 이두근을 사용하여 몸을 들어 올리세요.\",\"원하는 횟수만큼 반복합니다.\"],\"recordSetsDtos\":{\"2024-09-02\":[{\"reps\":10},{\"reps\":10}]}}}"))),
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
