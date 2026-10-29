package com.ticketbooking.controller;

import com.ticketbooking.dto.request.BookingRequest;
import com.ticketbooking.dto.response.ApiResponse;
import com.ticketbooking.dto.response.BookingResponse;
import com.ticketbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> bookTickets(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {

        BookingResponse booking = bookingService.bookTickets(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tickets booked successfully", booking));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> myBookings(Authentication authentication) {
        List<BookingResponse> bookings = bookingService.getMyBookings(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Bookings fetched successfully", bookings));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable Long id,
            Authentication authentication) {

        BookingResponse booking = bookingService.getBooking(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Booking fetched successfully", booking));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {

        BookingResponse booking = bookingService.cancelBooking(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", booking));
    }
}
