package com.ticketbooking.service;

import com.ticketbooking.config.CacheConfig;
import com.ticketbooking.dto.request.CreateEventRequest;
import com.ticketbooking.dto.request.EventFilter;
import com.ticketbooking.dto.response.EventResponse;
import com.ticketbooking.dto.response.PagedResponse;
import com.ticketbooking.entity.Event;
import com.ticketbooking.entity.User;
import com.ticketbooking.exception.BadRequestException;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.repository.EventRepository;
import com.ticketbooking.repository.UserRepository;
import com.ticketbooking.specification.EventSpecifications;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CacheService cacheService;

    public EventService(EventRepository eventRepository,
                        UserRepository userRepository,
                        CacheService cacheService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.cacheService = cacheService;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request, String organizerEmail) {
        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Event event = new Event();
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        event.setCategory(request.getCategory());
        event.setVenue(request.getVenue().trim());
        event.setEventDate(request.getEventDate());
        event.setTotalSeats(request.getTotalSeats());
        event.setAvailableSeats(request.getTotalSeats());   // nothing is booked yet
        event.setPrice(request.getPrice());
        event.setCreatedBy(organizer);

        Event saved = eventRepository.save(event);

        // a new event changes what every list would show
        cacheService.evictAllEventLists();

        return EventResponse.from(saved);
    }

    /**
     * Cached in Redis (cache "event", key = event id). The entry is removed whenever
     * seats are booked or released, see BookingService.
     */
    @Cacheable(cacheNames = CacheConfig.EVENT_CACHE, key = "#eventId")
    @Transactional(readOnly = true)
    public EventResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));
        return EventResponse.from(event);
    }

    /**
     * Cached per unique combination of filters (see EventFilter.cacheKey()).
     */
    @Cacheable(cacheNames = CacheConfig.EVENT_LIST_CACHE, key = "#filter.cacheKey()")
    @Transactional(readOnly = true)
    public PagedResponse<EventResponse> listEvents(EventFilter filter) {
        if (filter.getFrom() != null && filter.getTo() != null && filter.getFrom().isAfter(filter.getTo())) {
            throw new BadRequestException("'from' date cannot be after 'to' date");
        }

        // start with a condition that is always true, then add only the filters that were sent
        Specification<Event> spec = (root, query, cb) -> cb.conjunction();

        if (filter.getCategory() != null) {
            spec = spec.and(EventSpecifications.hasCategory(filter.getCategory()));
        }
        if (filter.getFrom() != null) {
            spec = spec.and(EventSpecifications.startsOnOrAfter(filter.getFrom().atStartOfDay()));
        }
        if (filter.getTo() != null) {
            // plusDays(1) so that the whole "to" day is included
            spec = spec.and(EventSpecifications.startsBefore(filter.getTo().plusDays(1).atStartOfDay()));
        }

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), Sort.by("eventDate").ascending());
        Page<Event> page = eventRepository.findAll(spec, pageable);

        List<EventResponse> items = new ArrayList<>();
        for (Event event : page.getContent()) {
            items.add(EventResponse.from(event));
        }

        return PagedResponse.from(page, items);
    }
}
