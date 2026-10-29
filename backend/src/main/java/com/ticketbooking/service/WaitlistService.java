package com.ticketbooking.service;

import com.ticketbooking.dto.response.WaitlistResponse;
import com.ticketbooking.entity.Booking;
import com.ticketbooking.entity.BookingStatus;
import com.ticketbooking.entity.Event;
import com.ticketbooking.entity.User;
import com.ticketbooking.entity.WaitlistEntry;
import com.ticketbooking.entity.WaitlistStatus;
import com.ticketbooking.exception.BadRequestException;
import com.ticketbooking.exception.ConflictException;
import com.ticketbooking.exception.ForbiddenException;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.repository.BookingRepository;
import com.ticketbooking.repository.EventRepository;
import com.ticketbooking.repository.UserRepository;
import com.ticketbooking.repository.WaitlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Waitlist for sold-out events. Every waitlist entry asks for exactly one ticket.
 * When a booking is cancelled, BookingService calls promoteFromWaitlist() and the
 * people at the front of the queue get a confirmed booking automatically.
 *
 * Lock order rule used everywhere: lock the EVENT row first. Because every method
 * follows the same order, two transactions can never end up waiting for each other.
 */
@Service
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CacheService cacheService;

    public WaitlistService(WaitlistRepository waitlistRepository,
                           EventRepository eventRepository,
                           UserRepository userRepository,
                           BookingRepository bookingRepository,
                           CacheService cacheService) {
        this.waitlistRepository = waitlistRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.cacheService = cacheService;
    }

    @Transactional
    public WaitlistResponse joinWaitlist(String email, Long eventId) {
        User user = findUser(email);

        // lock the event so seats cannot be released between our check and the insert
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));

        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This event has already taken place");
        }
        if (event.getAvailableSeats() > 0) {
            throw new ConflictException("Tickets are still available for this event, please book directly");
        }
        if (waitlistRepository.existsByUserIdAndEventIdAndStatus(user.getId(), eventId, WaitlistStatus.WAITING)) {
            throw new ConflictException("You are already on the waitlist for this event");
        }

        WaitlistEntry entry = new WaitlistEntry();
        entry.setUser(user);
        entry.setEvent(event);
        entry.setStatus(WaitlistStatus.WAITING);
        WaitlistEntry saved = waitlistRepository.save(entry);

        return WaitlistResponse.from(saved, calculatePosition(saved));
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> getMyWaitlist(String email) {
        User user = findUser(email);

        List<WaitlistResponse> result = new ArrayList<>();
        for (WaitlistEntry entry : waitlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId())) {
            Integer position = null;
            if (entry.getStatus() == WaitlistStatus.WAITING) {
                position = calculatePosition(entry);
            }
            result.add(WaitlistResponse.from(entry, position));
        }
        return result;
    }

    @Transactional
    public WaitlistResponse leaveWaitlist(String email, Long entryId) {
        User user = findUser(email);

        Long eventId = waitlistRepository.findEventIdByEntryId(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found with id " + entryId));

        // lock the event first, then load the entry, so we never act on a status that
        // a concurrent promotion has just changed
        eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));

        WaitlistEntry entry = waitlistRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found with id " + entryId));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only leave your own waitlist entries");
        }
        if (entry.getStatus() != WaitlistStatus.WAITING) {
            throw new ConflictException("This waitlist entry is no longer active");
        }

        entry.setStatus(WaitlistStatus.CANCELLED);
        waitlistRepository.save(entry);

        return WaitlistResponse.from(entry, null);
    }

    /**
     * Gives free seats to the people waiting, in the order they joined.
     * The caller must already hold the lock on the event row and be inside a transaction.
     *
     * @return the emails of the users who got a booking (their cached ticket list is now outdated)
     */
    public List<String> promoteFromWaitlist(Event event) {
        List<String> promotedEmails = new ArrayList<>();

        while (event.getAvailableSeats() > 0) {
            Optional<WaitlistEntry> next = waitlistRepository
                    .findFirstByEventIdAndStatusOrderByIdAsc(event.getId(), WaitlistStatus.WAITING);

            if (next.isEmpty()) {
                break;   // nobody left in the queue
            }

            WaitlistEntry entry = next.get();

            Booking booking = new Booking();
            booking.setReference(generateReference());
            booking.setUser(entry.getUser());
            booking.setEvent(event);
            booking.setQuantity(1);
            booking.setTotalPrice(event.getPrice());
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            event.setAvailableSeats(event.getAvailableSeats() - 1);

            entry.setStatus(WaitlistStatus.PROMOTED);
            waitlistRepository.save(entry);

            promotedEmails.add(entry.getUser().getEmail());
        }

        if (!promotedEmails.isEmpty()) {
            eventRepository.save(event);
        }
        return promotedEmails;
    }

    // position 1 = next in line
    private int calculatePosition(WaitlistEntry entry) {
        long peopleAhead = waitlistRepository.countByEventIdAndStatusAndIdLessThan(
                entry.getEvent().getId(), WaitlistStatus.WAITING, entry.getId());
        return (int) peopleAhead + 1;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String generateReference() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
