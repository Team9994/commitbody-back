package team9499.commitbody.domain.record.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team9499.commitbody.domain.record.dto.request.RecordRequest;
import team9499.commitbody.domain.record.service.RecordService;
import team9499.commitbody.global.authorization.domain.PrincipalDetails;
import team9499.commitbody.global.payload.SuccessResponse;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @PostMapping("/record")
    public ResponseEntity<?> saveRecord(@RequestBody RecordRequest recordRequest,
                                     @AuthenticationPrincipal PrincipalDetails principalDetails){
        Long memberId = principalDetails.getMember().getId();
        recordService.saveRecord(memberId, recordRequest.getRecordName(), recordRequest.getStartTime(),recordRequest.getEndTime(),recordRequest.getExercises());
        return ResponseEntity.ok(new SuccessResponse<>(true,"루틴 성공"));
    }

}
