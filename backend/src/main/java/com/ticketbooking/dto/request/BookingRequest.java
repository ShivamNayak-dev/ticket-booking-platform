package com.ticketbooking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BookingRequest {

    @NotNull(message = "Event id is required")
    private Long eventId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "You must book at least 1 ticket")
    @Max(value = 10, message = "You can book at most 10 tickets at a time")
    private Integer quantity;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
