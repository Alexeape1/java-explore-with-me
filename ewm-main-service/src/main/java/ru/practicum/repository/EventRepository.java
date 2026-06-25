package ru.practicum.repository;

import ru.practicum.model.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.EventState;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    @Query("SELECT e FROM Event e " +
            "WHERE (:#{#users == null} = true OR e.initiator.id IN :users) " +
            "AND (:#{#states == null} = true OR e.state IN :states) " +
            "AND (:#{#categories == null} = true OR e.category.id IN :categories) " +
            "AND (e.eventDate BETWEEN :rangeStart AND :rangeEnd)")
    List<Event> findEventsByAdmin(
            @Param("users") List<Long> users,
            @Param("states") List<EventState> states,
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    @Query("SELECT e FROM Event e WHERE " +
            "(:#{#text == null} = true OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) AND " +
            "(:#{#categories == null} = true OR e.category.id IN :categories) AND " +
            "(:#{#paid == null} = true OR e.paid = :paid) AND " +
            "(e.eventDate BETWEEN :rangeStart AND :rangeEnd) AND " +
            "e.state = :publishedState")
    List<Event> findEventsByPublic(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("publishedState") EventState publishedState,
            Pageable pageable);

    boolean existsByCategoryId(Long categoryId);
}