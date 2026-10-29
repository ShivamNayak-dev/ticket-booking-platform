package com.ticketbooking.dto.request;

import jakarta.validation.constraints.NotNull;

public class WaitlistRequest {

    @NotNull(message = "Event id is required")
    private Long eventId;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}
