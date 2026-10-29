package com.ticketbooking.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds the HttpOnly cookie that carries the JWT. HttpOnly means JavaScript on
 * the page cannot read it, which protects the token from XSS theft.
 */
@Component
public class AuthCookieService {

    private final String cookieName;
    private final boolean secure;
    private final JwtService jwtService;

    public AuthCookieService(@Value("${app.cookie.name}") String cookieName,
                             @Value("${app.cookie.secure}") boolean secure,
                             JwtService jwtService) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.jwtService = jwtService;
    }

    public ResponseCookie createAuthCookie(String token) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMillis(jwtService.getExpirationMs()))
                .build();
    }

    // same name and path with maxAge 0 tells the browser to delete the cookie
    public ResponseCookie createLogoutCookie() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String getCookieName() {
        return cookieName;
    }
}
