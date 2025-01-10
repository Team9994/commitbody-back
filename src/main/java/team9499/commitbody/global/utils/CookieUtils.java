package team9499.commitbody.global.utils;

import org.springframework.http.ResponseCookie;

import static team9499.commitbody.global.utils.TimeConverter.*;

public class CookieUtils {

    public static String visitorCookie(){
        return ResponseCookie.from("visitor")
                .secure(true)
                .path("/")
                .httpOnly(true)
                .sameSite("None")
                .maxAge(calculateMaxAge()).build().toString();
    }
}
