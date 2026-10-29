package com.ticketbooking.repository;

import com.ticketbooking.entity.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"event"})
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Only returns the id (a plain number, not an entity). Used so we can lock the
    // event row first and load the booking afterwards, which guarantees fresh data.
    @Query("select b.event.id from Booking b where b.id = :bookingId")
    Optional<Long> findEventIdByBookingId(@Param("bookingId") Long bookingId);
}
