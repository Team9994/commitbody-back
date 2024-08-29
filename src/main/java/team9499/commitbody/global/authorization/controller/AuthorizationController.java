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
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.NoSuchException;
import team9499.commitbody.global.authorization.dto.AdditionalInfoReqeust;
import team9499.commitbody.global.authorization.dto.JoinLoginRequest;
import team9499.commitbody.global.authorization.dto.RegisterNicknameRequest;
import team9499.commitbody.global.authorization.dto.TokenUserInfoResponse;
import team9499.commitbody.global.authorization.service.AuthorizationService;
import team9499.commitbody.global.payload.ErrorResponse;
import team9499.commitbody.global.payload.SuccessResponse;

import java.util.Map;


@Tag(name = "인증 인가",description = "인증 인가관련된 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    @Operation(summary = "회원가입/로그인", description = "매 요청마다 소셜 로그인 정보를 전달해야합니다. 최초 로그인 시에는 회원가입이 진행되며, 이후 로그인이 진행됩니다. 로그인 시에만 tokenInfo에 사용자 정보가 포함됩니다.(authMode: [로그인,회원가입])")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",description = "0K", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
            examples = @ExampleObject(value = "{\"success\":true,\"message\":\"성공\",\"data\":{\"refreshToken\":\"sample_refresh_token\",\"accessToken\":\"sample_access_token\",\"authMode\":\"타입 종류\",\"tokenInfo\":{\"memberId\":1,\"nickname\":\"닉네임\"}}}"))),
            @ApiResponse(responseCode = "400",description = "BADREQUEST", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "401",description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PostMapping("/auth")
    public ResponseEntity<SuccessResponse> socialLogin(@RequestBody JoinLoginRequest joinLoginRequest){
        Map<String, Object> jwtTokenMap = authorizationService.authenticateOrRegisterUser(joinLoginRequest.getLoginType(), joinLoginRequest.getSocialId());

        return ResponseEntity.ok().body(new SuccessResponse<>(true,"성공",jwtTokenMap));
    }

    @Operation(summary = "회원가입-추가정보", description = "회원가입의 필요한 추가 개인정보를 작성합니다. 추가정보 입력 완료시 tokenInfo에 사용자 정보가 포함됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                            examples = @ExampleObject(value = "{\"success\":true,\"message\":\"회원가입 성공\",\"data\":{\"tokenInfo\":{\"memberId\":1}}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 값 입렵 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\" : false,\"message\":{\"필드명\": \"오류 내용\"}}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PostMapping("/additional-info")
    public ResponseEntity<?> additionalInfo(@Valid @RequestBody AdditionalInfoReqeust additionalInfoReqeust, BindingResult result,HttpServletRequest request){
        String jwtToken = getJwtToken(request);

        TokenUserInfoResponse tokenUserInfoResponse = authorizationService.additionalInfoSave(
                additionalInfoReqeust.getNickName(), additionalInfoReqeust.getGender(), additionalInfoReqeust.getBirthday(), additionalInfoReqeust.getHeight(), additionalInfoReqeust.getWeight(),
                additionalInfoReqeust.getBodyFatPercentage(), additionalInfoReqeust.getBoneMineralDensity(), jwtToken
        );
        return ResponseEntity.ok(new SuccessResponse(true,"회원가입 성공",tokenUserInfoResponse));
    }

    @Operation(summary = "회원가입-닉네임 검증", description = "회원가입 시 닉네임을 검증합니다. 조건: 1. 영문+한글+숫자(3~8글자), 2. 영문+숫자(3~8글자), 3. 한글+숫자(3~8글자) 조건을 만족해야 한다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"멋진 닉네임입니다!\"}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - 값 입렵 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":{\"nickname\": \"닉네임 형식이 맞게 작성해 주세요\"}}"))),
            @ApiResponse(responseCode = "400_3", description = "BADREQUEST - 닉네임 중복 검증", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"중복된 닉네임 입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PostMapping("/register-nickname")
    public ResponseEntity<?> registerNickname(@Valid @RequestBody RegisterNicknameRequest registerNicknameRequest, BindingResult result){
        authorizationService.registerNickname(registerNicknameRequest.getNickname());
        return ResponseEntity.ok(new SuccessResponse<>(true,"멋진 닉네임입니다!"));
    }


    @Operation(summary = "엑세스 토큰 재발급", description = "엑세스 토큰 만료시 RefreshToken을 통해 AccessToken을 재발급 받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                    examples = @ExampleObject(value = "{\"success\":true,\"message\":\"재발급 성공\",\"data\":{\"accessToken\":\"accessToken_value\"}}"))),
            @ApiResponse(responseCode = "400_1", description = "BADREQUEST - 사용 불가 토큰",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"사용할 수 없는 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "400_2", description = "BADREQUEST - Authorization Null",content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"해당 정보를 찾을수 없습니다.\"}"))),
            @ApiResponse(responseCode = "401_2", description = "UNAUTHORIZED - 토큰 만료", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"만료된 토큰 입니다.\"}"))),
            @ApiResponse(responseCode = "401_1", description = "UNAUTHORIZED - 미존재 토큰 사용", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"success\" : false,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    @PostMapping("/auth-refresh")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request){
        String refreshToken = getJwtToken(request);
        Map<String, String> map = authorizationService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(new SuccessResponse<>(true,"재발급 성공",map));
    }

    private static String getJwtToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization==null) throw new NoSuchException(ExceptionStatus.BAD_REQUEST, ExceptionType.NO_SUCH_DATA);
        return authorization.replace("Bearer ", "");
    }
}
