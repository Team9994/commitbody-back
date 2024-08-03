package team9499.commitbody.global.utils;

import org.springframework.http.ResponseCookie;

public class CookieUtils {
    public static ResponseCookie createCookie(String cookieName, String value){
        int expiredTime = 0;
        if (cookieName.equals("refreshToken")){
            expiredTime = 60 * 60;;
        }else
            expiredTime = 30 * 24 * 60 * 60;

        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(expiredTime)
                .build();
    }
}
