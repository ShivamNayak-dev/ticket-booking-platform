package com.ticketbooking.controller;

import com.ticketbooking.dto.request.WaitlistRequest;
import com.ticketbooking.dto.response.ApiResponse;
import com.ticketbooking.dto.response.WaitlistResponse;
import com.ticketbooking.service.WaitlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WaitlistResponse>> joinWaitlist(
            @Valid @RequestBody WaitlistRequest request,
            Authentication authentication) {

        WaitlistResponse entry = waitlistService.joinWaitlist(authentication.getName(), request.getEventId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("You have been added to the waitlist", entry));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<WaitlistResponse>>> myWaitlist(Authentication authentication) {
        List<WaitlistResponse> entries = waitlistService.getMyWaitlist(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Waitlist fetched successfully", entries));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<WaitlistResponse>> leaveWaitlist(
            @PathVariable Long id,
            Authentication authentication) {

        WaitlistResponse entry = waitlistService.leaveWaitlist(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("You have left the waitlist", entry));
    }
}
