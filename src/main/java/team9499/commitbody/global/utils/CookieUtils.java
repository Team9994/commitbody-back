package team9499.commitbody.global.utils;

import org.springframework.http.ResponseCookie;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static team9499.commitbody.global.utils.TimeConverter.*;

public class CookieUtils {

    public static String visitorCookie(String nickname){
        String encodedValue = URLEncoder.encode(nickname, StandardCharsets.UTF_8);
        return ResponseCookie.from("visitor",encodedValue)
                .secure(true)
                .path("/")
                .httpOnly(true)
                .sameSite("None")
                .maxAge(calculateMaxAge()).build().toString();
    }
}
