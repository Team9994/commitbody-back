package team9499.commitbody.global.authorization.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.global.authorization.dto.AdditionalInfoReqeust;
import team9499.commitbody.global.authorization.dto.TokenUserInfoResponse;
import team9499.commitbody.global.authorization.service.AuthorizationService;
import team9499.commitbody.global.payload.SuccessResponse;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    @PostMapping("/auth")
    public ResponseEntity<SuccessResponse> socialLogin(@RequestParam("type")LoginType loginType,
                                         HttpServletRequest request){
        String jwtToken = getJwtToken(request);

        Map<String, Object> jwtTokenMap = authorizationService.authenticateOrRegisterUser(loginType, jwtToken);

        return ResponseEntity.ok().body(new SuccessResponse<>(true,"성공",jwtTokenMap));
    }

    @PostMapping("/additional-info")
    public ResponseEntity<?> additionalInfo(@Valid @RequestBody AdditionalInfoReqeust additionalInfoReqeust, BindingResult result,HttpServletRequest request){
        if (result.hasErrors()){
            Map<String,String> errorMap = new LinkedHashMap<>();
            for(FieldError error : result.getFieldErrors()){
                errorMap.put(error.getField(),error.getDefaultMessage());
            }
            return ResponseEntity.status(BAD_REQUEST).body(errorMap);
        }

        String jwtToken = getJwtToken(request);

        TokenUserInfoResponse tokenUserInfoResponse = authorizationService.additionalInfoSave(
                additionalInfoReqeust.getNickName(), additionalInfoReqeust.getGender(), additionalInfoReqeust.getBirthday(), additionalInfoReqeust.getHeight(), additionalInfoReqeust.getWeight(),
                additionalInfoReqeust.getBodyFatPercentage(), additionalInfoReqeust.getBoneMineralDensity(), jwtToken
        );
        return ResponseEntity.ok(new SuccessResponse(true,"회원가입 성공",tokenUserInfoResponse));
    }

    private static String getJwtToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization.replace("Bearer ", "");
    }
}
