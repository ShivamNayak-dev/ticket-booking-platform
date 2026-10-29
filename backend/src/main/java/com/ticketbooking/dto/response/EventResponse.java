package com.ticketbooking.dto.response;

import com.ticketbooking.entity.Event;
import com.ticketbooking.entity.EventCategory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String title,
        String description,
        EventCategory category,
        String venue,
        LocalDateTime eventDate,
        Integer totalSeats,
        Integer availableSeats,
        BigDecimal price,
        boolean soldOut,
        String organizerName,
        LocalDateTime createdAt
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getVenue(),
                event.getEventDate(),
                event.getTotalSeats(),
                event.getAvailableSeats(),
                event.getPrice(),
                event.getAvailableSeats() == 0,
                event.getCreatedBy().getName(),
                event.getCreatedAt()
        );
    }
}
