package team9499.commitbody.global.authorization.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import team9499.commitbody.domain.Member.domain.LoginType;
import team9499.commitbody.global.authorization.service.AuthorizationService;
import team9499.commitbody.global.config.SecurityConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@Import(SecurityConfig.class)
@WebMvcTest(AuthorizationController.class)
class AuthorizationControllerTest {

    @MockBean
    AuthorizationService authorizationService;
    @Autowired MockMvc mockMvc;

    @Test
    void socialLogin() throws Exception {
        String fakeToken ="Bearer eyJraWQiOiI5ZjI1MmRhZGQ1ZjIzM2Y5M2QyZmE1MjhkMTJmZWEiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiJjMGY1MzBhOGMyYmQwOGFiZGUzYTE5YTlkYmY2M2EyNyIsInN1YiI6IjMyODEyODI5NTciLCJhdXRoX3RpbWUiOjE3MjI1NDE5NjIsImlzcyI6Imh0dHBzOi8va2F1dGgua2FrYW8uY29tIiwibmlja25hbWUiOiLrr7zsmrAiLCJleHAiOjE3MjI1NjM1NjIsImlhdCI6MTcyMjU0MTk2MiwiZW1haWwiOiJrZXV5ZTYwMzhAbmF2ZXIuY29tIn0.hDSwIXqwYHCITLPrG5tcKO2DPljelsG6vOBUefmDWCd4cmhyT1Vgda6wCzn5_sJprilHdHTEzT4_h0R4-ccrICNrI3ErRP0Nh-Hycggn9Cwox42WZFtAGOk0IrO4umn0cDR0HIsJ_DHSs-F0FL_c5wpMNzxtxWj6xttqUEzZk9EcXZbj0KJYz_SXDwPkw359h0fX2JLnGolOXw9Hwyzmu6X8DkLxKDLgP6mnfqfZO5DU5Ql6ma9qFZnU3c_d5Xh5GWydupTXa-Co9PMYf8K-RwcXO46J7bKCKUc9iSUpRonWkVuaNDm3hB9Lo7pmajmNgcoSBFcMw_j6fC_UCBwC_w";
        given(authorizationService.authenticateOrRegisterUser(eq(LoginType.KAKAO),eq(fakeToken))).willReturn(Map.of("id","123415"));

        mockMvc.perform(post("/api/v1/social-login")
                .param("type", String.valueOf(LoginType.KAKAO))
                .header("Authorization",fakeToken))
                .andDo(print());

    }

}