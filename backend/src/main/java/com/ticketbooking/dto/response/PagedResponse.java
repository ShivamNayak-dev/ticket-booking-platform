package com.ticketbooking.dto.response;

import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Copies the numbers we need out of Spring's Page into a simple object that is safe to cache. */
    public static <T> PagedResponse<T> from(Page<?> page, List<T> content) {
        return new PagedResponse<>(
                new ArrayList<>(content),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
