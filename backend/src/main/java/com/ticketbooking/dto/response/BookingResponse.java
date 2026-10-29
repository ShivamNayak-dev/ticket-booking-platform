package com.ticketbooking.dto.response;

import com.ticketbooking.entity.Booking;
import com.ticketbooking.entity.BookingStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        String reference,
        Long eventId,
        String eventTitle,
        LocalDateTime eventDate,
        String venue,
        Integer quantity,
        BigDecimal totalPrice,
        BookingStatus status,
        LocalDateTime bookedAt
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getReference(),
                booking.getEvent().getId(),
                booking.getEvent().getTitle(),
                booking.getEvent().getEventDate(),
                booking.getEvent().getVenue(),
                booking.getQuantity(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
