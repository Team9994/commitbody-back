package team9499.commitbody.domain.record.controller;

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
import team9499.commitbody.domain.record.dto.request.RecordRequest;
import team9499.commitbody.domain.record.dto.response.RecordResponse;
import team9499.commitbody.domain.record.service.RecordService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;

@Tag(name = "기록",description = "운동 기록이 관련 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @Operation(summary = "기록 저장", description = "루틴 완료시 수행한 운동의 대해 기록을 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"루틴 성공\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 정보 미존재 시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"해당 정보를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 존재하지 않는 시용자 요청시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PostMapping("/record")
    public ResponseEntity<?> saveRecord(@RequestBody RecordRequest recordRequest,
                                     @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();
        recordService.saveRecord(memberId, recordRequest.getRecordName(), recordRequest.getStartTime(),recordRequest.getEndTime(),recordRequest.getExercises());
        return ResponseEntity.ok(new SuccessResponse<>(true,"루틴 성공"));
    }

    @GetMapping("/record/{id}")
    public ResponseEntity<?> getRecord(@PathVariable("id") Long id,
                                       @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();
        RecordResponse recordResponse = recordService.getRecord(id, memberId);
        return  ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",recordResponse));
    }

}
