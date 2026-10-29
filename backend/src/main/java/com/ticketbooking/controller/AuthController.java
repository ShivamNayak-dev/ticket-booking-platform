package com.ticketbooking.controller;

import com.ticketbooking.dto.request.LoginRequest;
import com.ticketbooking.dto.request.RegisterRequest;
import com.ticketbooking.dto.response.ApiResponse;
import com.ticketbooking.dto.response.UserResponse;
import com.ticketbooking.entity.User;
import com.ticketbooking.security.AuthCookieService;
import com.ticketbooking.security.JwtService;
import com.ticketbooking.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthCookieService cookieService;

    public AuthController(AuthService authService, JwtService jwtService, AuthCookieService cookieService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        String token = jwtService.generateToken(user);

        // the user is logged in right after registering
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookieService.createAuthCookie(token).toString())
                .body(ApiResponse.success("Registration successful", UserResponse.from(user)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request);
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.createAuthCookie(token).toString())
                .body(ApiResponse.success("Login successful", UserResponse.from(user)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.createLogoutCookie().toString())
                .body(ApiResponse.success("Logged out successfully", null));
    }

    // the frontend calls this on page load to find out whether the cookie is still valid
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> currentUser(Authentication authentication) {
        User user = authService.getByEmail(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Current user", UserResponse.from(user)));
    }
}
