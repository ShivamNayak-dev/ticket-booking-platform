package com.ticketbooking.specification;

import com.ticketbooking.entity.Event;
import com.ticketbooking.entity.EventCategory;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Small building blocks for the event search. Each method returns one
 * condition and the service joins together only the ones the user asked for.
 */
public class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> hasCategory(EventCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    // inclusive start
    public static Specification<Event> startsOnOrAfter(LocalDateTime start) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDateTime>get("eventDate"), start);
    }

    // exclusive end, so "to = 10 Dec" includes everything that happens on 10 Dec
    public static Specification<Event> startsBefore(LocalDateTime end) {
        return (root, query, cb) -> cb.lessThan(root.<LocalDateTime>get("eventDate"), end);
    }
}
