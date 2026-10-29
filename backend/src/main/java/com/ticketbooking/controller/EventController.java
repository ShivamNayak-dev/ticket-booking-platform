package com.ticketbooking.controller;

import com.ticketbooking.dto.request.CreateEventRequest;
import com.ticketbooking.dto.request.EventFilter;
import com.ticketbooking.dto.response.ApiResponse;
import com.ticketbooking.dto.response.EventResponse;
import com.ticketbooking.dto.response.PagedResponse;
import com.ticketbooking.entity.EventCategory;
import com.ticketbooking.service.EventService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * GET /api/events?category=MUSIC&from=2026-12-01&to=2026-12-31&page=0&size=10
     * All parameters are optional.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<EventResponse>>> listEvents(
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        EventFilter filter = new EventFilter(category, from, to, page, size);
        PagedResponse<EventResponse> events = eventService.listEvents(filter);

        return ResponseEntity.ok(ApiResponse.success("Events fetched successfully", events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable Long id) {
        EventResponse event = eventService.getEvent(id);
        return ResponseEntity.ok(ApiResponse.success("Event fetched successfully", event));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication) {

        EventResponse created = eventService.createEvent(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event created successfully", created));
    }
}
