package com.ticketbooking.repository;

import com.ticketbooking.entity.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    /**
     * Loads the event with "SELECT ... FOR UPDATE". Any other transaction that
     * tries to lock the same row has to wait until we commit or roll back.
     * This is what stops two people from grabbing the last seat at the same time.
     * Must be called inside a @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    // fetch the organizer together with the events so the list does not fire one extra query per row
    @Override
    @EntityGraph(attributePaths = {"createdBy"})
    Page<Event> findAll(Specification<Event> spec, Pageable pageable);
}
