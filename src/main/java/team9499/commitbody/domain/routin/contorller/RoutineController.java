package team9499.commitbody.domain.routin.contorller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.routin.dto.response.MyRoutineResponse;
import team9499.commitbody.domain.routin.dto.rqeust.RoutineRequest;
import team9499.commitbody.domain.routin.dto.rqeust.UpdateRoutineRequest;
import team9499.commitbody.domain.routin.service.RoutineService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;

@Tag(name = "운동 루틴", description = "운동 루틴 관련")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class RoutineController {

    private final RoutineService routineService;

    @Operation(summary = "루틴 등록", description = "사용자는 운동 목록을 통해 루틴을 등록가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"루틴 등록 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 해당 정보 미존재",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"해당 정보를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "400-3",description = "BADREQUEST - 사용할수 없는 토큰", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PostMapping("/routine")
    public ResponseEntity<?> saveRoutine(@RequestBody RoutineRequest routineRequest,
                                      @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        routineService.saveRoutine(memberId,routineRequest.getDefaults(),routineRequest.getCustoms(),routineRequest.getRoutineName());
        return ResponseEntity.ok(new SuccessResponse<>(true,"루틴 등록 성공"));
    }

    @Operation(summary = "루틴 조회", description = "사용자가 지정한 루틴의 정보를 조회합니다. 커스텀운동의 대한 exerciseType은 무게와 횟수 로 고정됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"루틴 등록 성공\",\"data\":{\"routineDtos\":[{\"routineId\":1,\"routineName\":\"루틴제목\",\"targets\":[\"복근\",\"등\"],\"exercises\":[{\"exerciseId\":1,\"exerciseName\":\"3/4 싯업\",\"gifUrl\":\"https://v2.exercisedb.io/image/oAVJS-wlSfNhXd\",\"exerciseType\":\"횟수\",\"sets\":4,\"orders\" : 1,\"routineSets\" : [{ \"setsId\" : 105, \"sets\" : 1}]},{\"customExerciseId\":1,\"exerciseName\":\"커스텀운동명\",\"gifUrl\":\"http://example.com/pushup.gif\",\"exerciseType\":\"무게와 횟수\",\"sets\":4,\"orders\":2,\"routineSets\" : [{ \"setsId\" : 105, \"sets\" : 2}]}]}]}}"))),
            @ApiResponse(responseCode = "400-3",description = "BADREQUEST - 사용할수 없는 토큰", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @GetMapping("/routine")
    public ResponseEntity<?> getRoutine(@AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        MyRoutineResponse myRoutine = routineService.getMyRoutine(memberId);
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",myRoutine));
    }


    @Operation(summary = "루틴 편집", description = "사용자가 등록한 루틴을 편집합니다. 필드 별 변경할 데이터가 있는 경우에만 해당 필드를 작성합니다. 변경할 데이터가 없는 경우에는 해당 필드를 작성하지 않습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"루틴 수정 완료\"}"))),
            @ApiResponse(responseCode = "400-1",description = "BADREQUEST - 사용할수 없는 토큰", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400-2",description = "BADREQUEST - 데이터 존재하지 않을시", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"해당 정보를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PutMapping("/routine/{id}")
    public ResponseEntity<?> updateRoutine(@PathVariable("id")Long id,
                                           @RequestBody UpdateRoutineRequest updateRoutineRequest,
                                           @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        routineService.updateRoutine(id,memberId,updateRoutineRequest.getUpdateRoutineName(),updateRoutineRequest.getDeleteRoutines(),updateRoutineRequest.getUpdateSets(),
                updateRoutineRequest.getDeleteSets(),updateRoutineRequest.getNewExercises(),updateRoutineRequest.getChangeExercises(),updateRoutineRequest.getChangeOrders());
        return ResponseEntity.ok(new SuccessResponse<>(true,"루틴 수정 완료"));
    }

    private static Long getMemberId(PrincipalDetails principalDetails) {
        return principalDetails.getMember().getId();
    }

    @DeleteMapping("/routine/{id}")
    public ResponseEntity<?> deleteRoutine(@PathVariable("id")Long id,
                                           @AuthenticationPrincipal PrincipalDetails principalDetails){
        routineService.deleteRoutine(id,getMemberId(principalDetails));
        return ResponseEntity.ok(new SuccessResponse<>(true,"삭제 성공"));
    }
}
