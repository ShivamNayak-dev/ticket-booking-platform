package com.ticketbooking.dto.request;

import com.ticketbooking.entity.EventCategory;

import java.time.LocalDate;

/**
 * Search parameters for the event list. Page and size are cleaned up in the
 * constructor so a bad value from the URL can never reach the database.
 */
public class EventFilter {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final EventCategory category;
    private final LocalDate from;
    private final LocalDate to;
    private final int page;
    private final int size;

    public EventFilter(EventCategory category, LocalDate from, LocalDate to, int page, int size) {
        this.category = category;
        this.from = from;
        this.to = to;
        this.page = Math.max(page, 0);

        if (size < 1) {
            this.size = DEFAULT_PAGE_SIZE;
        } else {
            this.size = Math.min(size, MAX_PAGE_SIZE);
        }
    }

    public EventCategory getCategory() {
        return category;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    /** Same filter => same key, so identical searches share one Redis entry. */
    public String cacheKey() {
        return "category=" + category + "|from=" + from + "|to=" + to + "|page=" + page + "|size=" + size;
    }
}
