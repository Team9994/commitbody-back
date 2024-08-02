package team9499.commitbody.global.authorization.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.global.authorization.service.AuthorizationService;
import team9499.commitbody.global.payload.SuccessResponse;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    @PostMapping("/auth")
    public ResponseEntity<?> socialLogin(@RequestParam("type")LoginType loginType,
                                         HttpServletRequest request){
        String jwtToken = getJwtToken(request);

        Map<String, String> jwtTokenMap = authorizationService.authenticateOrRegisterUser(loginType, jwtToken);

        return ResponseEntity.ok().body(new SuccessResponse<>(true,"성공",jwtTokenMap));
    }


    private static String getJwtToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization.replace("Bearer ", "");
    }
}
