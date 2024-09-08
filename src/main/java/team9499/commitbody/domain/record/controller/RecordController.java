package team9499.commitbody.domain.record.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.record.dto.request.RecordRequest;
import team9499.commitbody.domain.record.dto.request.UpdateRecordRequest;
import team9499.commitbody.domain.record.dto.response.RecordMonthResponse;
import team9499.commitbody.domain.record.dto.response.RecordResponse;
import team9499.commitbody.domain.record.service.RecordService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;

import java.time.LocalDateTime;

@Tag(name = "기록",description = "운동 기록이 관련 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;


    @Operation(summary = "기록 저장", description = "루틴 완료시 수행한 운동의 대해 기록을 저장합니다. 저장시 정장된 recordId를 반환합니다.(exercises 배열안의 'recordId', 'endTime', 'recordName' 필드값은 사용하지 않습니다.)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"루틴 성공\",\"data\":1}"))),
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
        Long memberId = getMemberId(principalDetails);
        Long recordId = recordService.saveRecord(memberId, recordRequest.getRecordName(), recordRequest.getStartTime(), recordRequest.getEndTime(), recordRequest.getExercises());
        return ResponseEntity.ok(new SuccessResponse<>(true,"루틴 성공",recordId));
    }

    @Operation(summary = "기록 상세 조회", description = "사용자가 완료한 기록의 대한 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"조회 성공\",\"data\":{\"recordId\":1,\"recordName\":\"나의 첫 번째 기록\",\"startDate\":\"2024.08.14.(수)\",\"durationTime\":\"6:46~8:46\",\"duration\":120,\"recordVolume\":85,\"recordSets\":11,\"recordCalorie\":414,\"details\":[{\"recordDetailId\":1,\"exerciseId\":1,\"gifUrl\":\"https://example.com\",\"detailsReps\":12,\"detailsSets\":2,\"maxReps\":7,\"sets\":[{\"setId\":191,\"reps\":6},{\"setId\":192,\"reps\":7}]}]}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 정보 미존재 시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"해당 정보를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 존재하지 않는 시용자 요청시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용자를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @GetMapping("/record/{id}")
    public ResponseEntity<?> getRecord(@PathVariable("id") Long id,
                                       @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        RecordResponse recordResponse = recordService.getRecord(id, memberId);
        return  ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",recordResponse));
    }

    @Operation(summary = "기록 수정", description = "사용자가 완료한 기록의 정보를 수정 합니다.(운동 순서, 새 운동 추가,운동 삭제, 세트 추가/삭제/업데이트)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"기록 수정 완료\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 정보 미존재 시",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"해당 정보를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PutMapping("/record/{recordId}")
    public ResponseEntity<?> updateRoutine(@Parameter(description = "기록 ID") @PathVariable("recordId")Long recordId,
                                           @RequestBody UpdateRecordRequest updateV2RecordRequest,
                                           @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);

        recordService.updateRecord(memberId,recordId,updateV2RecordRequest.getRecordName(),updateV2RecordRequest.getRecordDtoList());
        return ResponseEntity.ok(new SuccessResponse<>(true,"기록 수정 완료"));
    }

    @Operation(summary = "기록 삭제", description = "작성자만이 해당 기록을 삭제 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"삭제 성공\"}"))),
            @ApiResponse(responseCode = "400", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "타 사용자가 삭제시", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"작성자만 이용할 수 있습니다.\"}")))})
    @DeleteMapping("/record/{recordId}")
    public ResponseEntity<?> deleteRecord(@Parameter(description = "기록 ID")@PathVariable("recordId") Long recordId,
                                          @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        recordService.deleteRecord(memberId,recordId);
        return ResponseEntity.ok(new SuccessResponse<>(true,"삭제 성공"));
    }

    @Operation(summary = "기록 조회", description = "사용자가 진행한 기록의 대해 모두 조회합니다. Default : size - 10, lastTime은 다음페이지 존재시 마지막 시간을 보냅니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"조회 성공\",\"data\":{\"dayRecordCount\":{\"1\":{\"day\":\"2024.08.01.(목)\",\"recordDays\":[{\"recordId\":56,\"recordName\":\"테스트\",\"durationTime\":\"2024.08.01.(목) · 18:37~20:37\"}]},\"3\":{\"day\":\"2024.08.03.(토)\",\"recordDays\":[{\"recordId\":58,\"recordName\":\"테스트\",\"durationTime\":\"2024.08.03.(토) · 18:37~20:37\"}]}},\"recordPage\":{\"hasNext\":false,\"records\":[{\"recordId\":57,\"recordName\":\"테스트\",\"durationTime\":\"2024.08.01.(목) · 18:37~20:37\",\"lastTime\":\"2024-08-01T20:37:32.71\"}]}}}"))),
            @ApiResponse(responseCode = "400", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))})
    @GetMapping("/record")
    public ResponseEntity<?> get(@RequestParam(value = "lastTime",required = false) LocalDateTime lastTime,
                                 @Parameter(example = "{\"size\":10}")@PageableDefault(size = 10) Pageable pageable,
                                 @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = getMemberId(principalDetails);
        RecordMonthResponse recordForMember = recordService.getRecordForMember(memberId,lastTime,pageable);
        return ResponseEntity.ok(new SuccessResponse<>(true,"조회 성공",recordForMember));
    }

    private static Long getMemberId(PrincipalDetails principalDetails) {
        Long memberId = principalDetails.getMember().getId();
        return memberId;
    }
}
