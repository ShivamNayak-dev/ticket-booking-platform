package com.ticketbooking.repository;

import com.ticketbooking.entity.WaitlistEntry;
import com.ticketbooking.entity.WaitlistStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    // the person who has been waiting the longest (lowest id) is first in line
    Optional<WaitlistEntry> findFirstByEventIdAndStatusOrderByIdAsc(Long eventId, WaitlistStatus status);

    boolean existsByUserIdAndEventIdAndStatus(Long userId, Long eventId, WaitlistStatus status);

    long countByEventIdAndStatusAndIdLessThan(Long eventId, WaitlistStatus status, Long id);

    @EntityGraph(attributePaths = {"event"})
    List<WaitlistEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select w.event.id from WaitlistEntry w where w.id = :entryId")
    Optional<Long> findEventIdByEntryId(@Param("entryId") Long entryId);
}
