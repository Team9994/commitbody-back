package team9499.commitbody.domain.exercise.contorller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team9499.commitbody.domain.exercise.dto.CustomExerciseReqeust;
import team9499.commitbody.domain.exercise.dto.SearchExerciseResponse;
import team9499.commitbody.domain.exercise.event.ElasticSaveExerciseEvent;
import team9499.commitbody.domain.exercise.service.ExerciseService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
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


    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveExercise(@RequestPart(name = "customExerciseReqeust", required = false) CustomExerciseReqeust customExerciseReqeust,
                                          @RequestPart(name ="file" , required = false) MultipartFile file,
                                          @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long id = principalDetails.getMember().getId();
        Long customExerciseId = exerciseService.saveCustomExercise(customExerciseReqeust.getExerciseName(), customExerciseReqeust.getExerciseTarget(),
                customExerciseReqeust.getExerciseEquipment(), id, file);

        eventPublisher.publishEvent(new ElasticSaveExerciseEvent(customExerciseId));

        return ResponseEntity.ok(new SuccessResponse<>(true,"저장 성공"));

    }
}
