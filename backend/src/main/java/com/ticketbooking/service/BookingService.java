package com.ticketbooking.service;

import com.ticketbooking.config.CacheConfig;
import com.ticketbooking.dto.request.BookingRequest;
import com.ticketbooking.dto.response.BookingResponse;
import com.ticketbooking.entity.Booking;
import com.ticketbooking.entity.BookingStatus;
import com.ticketbooking.entity.Event;
import com.ticketbooking.entity.User;
import com.ticketbooking.exception.BadRequestException;
import com.ticketbooking.exception.ConflictException;
import com.ticketbooking.exception.ForbiddenException;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.repository.BookingRepository;
import com.ticketbooking.repository.EventRepository;
import com.ticketbooking.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final WaitlistService waitlistService;
    private final CacheService cacheService;

    public BookingService(BookingRepository bookingRepository,
                          EventRepository eventRepository,
                          UserRepository userRepository,
                          WaitlistService waitlistService,
                          CacheService cacheService) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.waitlistService = waitlistService;
        this.cacheService = cacheService;
    }

    /**
     * Books tickets with a pessimistic lock on the event row.
     *
     * Flow: lock event row -> check seats -> reduce seats -> save booking -> commit.
     * If two users try to buy the last seat together, the second one waits at the lock,
     * then sees 0 seats left and gets a clean "sold out" error instead of a double booking.
     */
    @Transactional
    public BookingResponse bookTickets(String email, BookingRequest request) {
        User user = findUser(email);

        Event event = eventRepository.findByIdForUpdate(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + request.getEventId()));

        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This event has already taken place");
        }

        int requested = request.getQuantity();
        int available = event.getAvailableSeats();

        if (available == 0) {
            throw new ConflictException("This event is sold out. You can join the waitlist.");
        }
        if (requested > available) {
            throw new ConflictException("Only " + available + " ticket(s) left, you asked for " + requested);
        }

        event.setAvailableSeats(available - requested);
        eventRepository.save(event);

        Booking booking = new Booking();
        booking.setReference(generateReference());
        booking.setUser(user);
        booking.setEvent(event);
        booking.setQuantity(requested);
        booking.setTotalPrice(event.getPrice().multiply(BigDecimal.valueOf(requested)));
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);

        // seat count changed, so the cached copies of this event and of the lists are outdated
        cacheService.evictEvent(event.getId());
        cacheService.evictAllEventLists();
        cacheService.evictUserBookings(email);

        return BookingResponse.from(saved);
    }

    /**
     * Cached per user (cache "userBookings", key = email, short TTL).
     */
    @Cacheable(cacheNames = CacheConfig.USER_BOOKINGS_CACHE, key = "#email")
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String email) {
        User user = findUser(email);

        List<BookingResponse> result = new ArrayList<>();
        for (Booking booking : bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId())) {
            result.add(BookingResponse.from(booking));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(String email, Long bookingId) {
        User user = findUser(email);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only view your own bookings");
        }
        return BookingResponse.from(booking);
    }

    /**
     * Cancels a booking, gives the seats back and offers them to the waitlist.
     */
    @Transactional
    public BookingResponse cancelBooking(String email, Long bookingId) {
        User user = findUser(email);

        Long eventId = bookingRepository.findEventIdByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));

        // lock the event first (same order as in bookTickets) and only then read the booking,
        // so two parallel cancel requests cannot both give the same seats back
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only cancel your own bookings");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("This booking is already cancelled");
        }
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Bookings for past events cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        event.setAvailableSeats(event.getAvailableSeats() + booking.getQuantity());
        eventRepository.save(event);

        // freed seats go to the people waiting, if there are any
        List<String> promotedEmails = waitlistService.promoteFromWaitlist(event);

        cacheService.evictEvent(eventId);
        cacheService.evictAllEventLists();
        cacheService.evictUserBookings(email);
        for (String promotedEmail : promotedEmails) {
            cacheService.evictUserBookings(promotedEmail);
        }

        return BookingResponse.from(booking);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String generateReference() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
