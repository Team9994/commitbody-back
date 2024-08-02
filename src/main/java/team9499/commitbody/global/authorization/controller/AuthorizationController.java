package team9499.commitbody.global.authorization.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.global.authorization.service.AuthorizationService;
import team9499.commitbody.global.payload.SuccessResponse;
import team9499.commitbody.global.utils.CookieUtils;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    private final String ACCESS_TOKEN = "accessToken";
    private final String REFRESH_TOKEN = "refreshToken";

    @PostMapping("/auth")
    public ResponseEntity<?> socialLogin(@RequestParam("type")LoginType loginType,
                                         @RequestHeader("Authorization") String authorization){
        String jwtToken = authorization.replace("Bearer ", "");
        Map<String, String> map = authorizationService.authenticateOrRegisterUser(loginType, jwtToken);

        ResponseCookie accessCookie = CookieUtils.createCookie(ACCESS_TOKEN, map.get(ACCESS_TOKEN));
        ResponseCookie refreshCookie = CookieUtils.createCookie(REFRESH_TOKEN, map.get(REFRESH_TOKEN));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE,refreshCookie.toString())
                .body(new SuccessResponse<>(true,"성공"));
    }
}
