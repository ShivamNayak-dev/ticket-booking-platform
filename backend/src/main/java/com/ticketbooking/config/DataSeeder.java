package com.ticketbooking.config;

import com.ticketbooking.entity.Event;
import com.ticketbooking.entity.EventCategory;
import com.ticketbooking.entity.User;
import com.ticketbooking.repository.EventRepository;
import com.ticketbooking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ticketbooking.entity.Role;

/**
 * Puts a demo organizer and a handful of events into an empty database so the
 * app has something to show on the first run. Switch it off with app.seed.enabled=false.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String DEMO_EMAIL = "demo@ticketbook.com";
    private static final String DEMO_PASSWORD = "Demo@1234";

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedEnabled;

    public DataSeeder(UserRepository userRepository,
                      EventRepository eventRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.enabled:true}") boolean seedEnabled) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled || eventRepository.count() > 0) {
            return;
        }

        User organizer = userRepository.findByEmail(DEMO_EMAIL).orElseGet(() -> {
    User user = new User(
            "Demo Organizer",
            DEMO_EMAIL,
            passwordEncoder.encode(DEMO_PASSWORD)
    );
    user.setRole(Role.ORGANIZER);
    return userRepository.save(user);
});

if (organizer.getRole() != Role.ORGANIZER) {
    organizer.setRole(Role.ORGANIZER);
    userRepository.save(organizer);
}

        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        eventRepository.save(buildEvent(organizer,
                "Sunburn Arena Live", EventCategory.MUSIC, "Jawaharlal Nehru Stadium, New Delhi",
                now.plusDays(12).withHour(19), 500, "1499.00",
                "A night of electronic music with top DJs from across the country."));

        eventRepository.save(buildEvent(organizer,
                "India vs Australia - T20", EventCategory.SPORTS, "Narendra Modi Stadium, Ahmedabad",
                now.plusDays(20).withHour(18), 800, "2500.00",
                "Catch the T20 clash live from the stands."));

        eventRepository.save(buildEvent(organizer,
                "DevConf 2026", EventCategory.TECH, "Bangalore International Centre",
                now.plusDays(35).withHour(9), 300, "999.00",
                "Two days of talks and workshops on backend engineering, cloud and AI."));

        eventRepository.save(buildEvent(organizer,
                "Stand-up Special: Laugh Riot", EventCategory.COMEDY, "Siri Fort Auditorium, New Delhi",
                now.plusDays(7).withHour(20), 150, "799.00",
                "An evening with four of the best stand-up comedians."));

        eventRepository.save(buildEvent(organizer,
                "Hamlet - The Stage Play", EventCategory.THEATRE, "NCPA, Mumbai",
                now.plusDays(28).withHour(19), 200, "650.00",
                "A modern retelling of Shakespeare's classic."));

        eventRepository.save(buildEvent(organizer,
                "Startup Founders Summit", EventCategory.CONFERENCE, "The Leela Palace, Chennai",
                now.plusDays(50).withHour(10), 250, "1999.00",
                "Meet founders and investors and learn how they built their companies."));

        // tiny event so the waitlist feature is easy to try: book both seats, then join the waitlist
        eventRepository.save(buildEvent(organizer,
                "Intimate Jazz Night", EventCategory.MUSIC, "Blue Frog, Gurugram",
                now.plusDays(5).withHour(21), 2, "300.00",
                "A very small private jazz session. Only two seats are available."));

        log.info("Demo data created. Login with {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
    }

    private Event buildEvent(User organizer, String title, EventCategory category, String venue,
                             LocalDateTime date, int seats, String price, String description) {
        Event event = new Event();
        event.setTitle(title);
        event.setCategory(category);
        event.setVenue(venue);
        event.setEventDate(date);
        event.setTotalSeats(seats);
        event.setAvailableSeats(seats);
        event.setPrice(new BigDecimal(price));
        event.setDescription(description);
        event.setCreatedBy(organizer);
        return event;
    }
}
