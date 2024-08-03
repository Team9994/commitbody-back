package team9499.commitbody.global.authorization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@Tag(name = "인증 인가",description = "인증 인가관련된 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    @Operation(summary = "회원가입/로그인", description = "매 요청마다 OpenID를 type에 전달합니다. 최초 로그인 시에는 회원가입이 진행되며, 이후 로그인이 진행됩니다. 로그인 시에만 tokenInfo에 사용자 정보가 포함됩니다.(authMode: [로그인,회원가입])")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",description = "0K", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
            examples = @ExampleObject(value = "{\"success\":true,\"message\":\"성공\",\"data\":{\"refreshToken\":\"sample_refresh_token\",\"accessToken\":\"sample_access_token\",\"authMode\":\"타입 종류\",\"tokenInfo\":{\"memberId\":1}}}"))),
            @ApiResponse(responseCode = "400",description = "BADREQUEST", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401",description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PostMapping("/auth")
    public ResponseEntity<SuccessResponse> socialLogin(@RequestParam("type")LoginType loginType,
                                         HttpServletRequest request){
        String jwtToken = getJwtToken(request);

        Map<String, Object> jwtTokenMap = authorizationService.authenticateOrRegisterUser(loginType, jwtToken);

        return ResponseEntity.ok().body(new SuccessResponse<>(true,"성공",jwtTokenMap));
    }

    @Operation(summary = "회원가입-추가정보", description = "회원가입의 필요한 추가 개인정보를 작성합니다. 추가정보 입력 완료시 tokenInfo에 사용자 정보가 포함됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                            examples = @ExampleObject(value = "{\"success\":true,\"message\":\"회원가입 성공\",\"data\":{\"tokenInfo\":{\"memberId\":1}}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 값 입렵 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"실패\",\"data\":{\"필드명\": \"오류 내용\"}}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })

    @PostMapping("/additional-info")
    public ResponseEntity<?> additionalInfo(@Valid @RequestBody AdditionalInfoReqeust additionalInfoReqeust, BindingResult result,HttpServletRequest request){
        if (result.hasErrors()){
            Map<String,String> errorMap = new LinkedHashMap<>();
            for(FieldError error : result.getFieldErrors()){
                errorMap.put(error.getField(),error.getDefaultMessage());
            }
            return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse<>(false,"실패",errorMap));
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
