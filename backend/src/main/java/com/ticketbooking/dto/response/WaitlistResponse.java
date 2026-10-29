package com.ticketbooking.dto.response;

import com.ticketbooking.entity.WaitlistEntry;
import com.ticketbooking.entity.WaitlistStatus;

import java.time.LocalDateTime;

/**
 * position is only filled while the status is WAITING (1 = next in line),
 * for promoted or cancelled entries it is null.
 */
public record WaitlistResponse(
        Long id,
        Long eventId,
        String eventTitle,
        LocalDateTime eventDate,
        WaitlistStatus status,
        Integer position,
        LocalDateTime joinedAt
) {

    public static WaitlistResponse from(WaitlistEntry entry, Integer position) {
        return new WaitlistResponse(
                entry.getId(),
                entry.getEvent().getId(),
                entry.getEvent().getTitle(),
                entry.getEvent().getEventDate(),
                entry.getStatus(),
                position,
                entry.getCreatedAt()
        );
    }
}
