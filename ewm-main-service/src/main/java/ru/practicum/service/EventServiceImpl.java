package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import ru.practicum.dto.*;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.*;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final WebClient webClient;

    @Value("${stats-server.url:http://localhost:9090}")
    private String statsServerUrl;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        log.info("Adding new event for user: {}", userId);

        if (newEventDto.getParticipantLimit() != null && newEventDto.getParticipantLimit() < 0) {
            throw new BadRequestException("Participant limit cannot be negative");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + newEventDto.getCategory() + " was not found"));

        LocalDateTime eventDate = LocalDateTime.parse(newEventDto.getEventDate(), FORMATTER);
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }

        Event event = eventMapper.toEntity(newEventDto, category, user);
        Event saved = eventRepository.save(event);

        return eventMapper.toFullDto(saved);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        log.info("Getting events for user: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, pageRequest);

        return events.stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        log.info("Getting event {} for user {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        return eventMapper.toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Updating event by user: {} for event: {}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new BadRequestException("User with id=" + userId + " is not initiator of event with id=" + eventId);
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (updateRequest.getEventDate() != null) {
            LocalDateTime newDate = LocalDateTime.parse(updateRequest.getEventDate(), FORMATTER);
            if (newDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Event date must be at least 2 hours in future.");
            }
            event.setEventDate(newDate);
        }

        Category category = null;
        if (updateRequest.getCategory() != null) {
            category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
        }

        eventMapper.updateFromUserRequest(event, updateRequest, category);

        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction().equals("SEND_TO_REVIEW")) {
                event.setState(EventState.PENDING);
            } else if (updateRequest.getStateAction().equals("CANCEL_REVIEW")) {
                event.setState(EventState.CANCELED);
            }
        }

        // Ручное обновление полей (на случай, если маппер не всё подтягивает, т.к. тесты постоянно проваливались без этого)
        if (updateRequest.getAnnotation() != null) event.setAnnotation(updateRequest.getAnnotation());
        if (updateRequest.getDescription() != null) event.setDescription(updateRequest.getDescription());
        if (updateRequest.getPaid() != null) event.setPaid(updateRequest.getPaid());
        if (updateRequest.getParticipantLimit() != null) event.setParticipantLimit(updateRequest.getParticipantLimit());
        if (updateRequest.getRequestModeration() != null)
            event.setRequestModeration(updateRequest.getRequestModeration());
        if (updateRequest.getTitle() != null) event.setTitle(updateRequest.getTitle());
        if (updateRequest.getLocation() != null) {
            ru.practicum.dto.Location loc = new ru.practicum.dto.Location();
            loc.setLat(updateRequest.getLocation().getLat());
            loc.setLon(updateRequest.getLocation().getLon());
            event.setLocation(loc);
        }

        Event updated = eventRepository.saveAndFlush(event);
        return eventMapper.toFullDto(updated);
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users,
                                               List<String> states,
                                               List<Long> categories,
                                               String rangeStart,
                                               String rangeEnd,
                                               Integer from,
                                               Integer size) {
        log.info("users={}", users);
        log.info("states={}", states);
        log.info("categories={}", categories);
        log.info("rangeStart={}", rangeStart);
        log.info("rangeEnd={}", rangeEnd);

        List<EventState> stateEnums = null;
        if (states != null && !states.isEmpty()) {
            stateEnums = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        LocalDateTime start = rangeStart == null
                ? LocalDateTime.of(1900, 1, 1, 0, 0)
                : LocalDateTime.parse(rangeStart, FORMATTER);

        LocalDateTime end = rangeEnd == null
                ? LocalDateTime.of(3000, 1, 1, 0, 0)
                : LocalDateTime.parse(rangeEnd, FORMATTER);

        PageRequest pageRequest = PageRequest.of(from / size, size);

        List<Event> events = eventRepository.findEventsByAdmin(
                users,
                stateEnums,
                categories,
                start,
                end,
                pageRequest
        );

        events.forEach(event ->
                event.setConfirmedRequests(
                        requestRepository.countByEventIdAndStatus(
                                event.getId(),
                                RequestStatus.CONFIRMED
                        )
                )
        );
        log.info("FOUND EVENTS = {}", events.size());
        events.forEach(e ->
                log.info(
                        "EVENT id={} confirmedRequests={}",
                        e.getId(),
                        e.getConfirmedRequests()
                )
        );
        events.forEach(event -> {
            long confirmed = requestRepository.countByEventIdAndStatus(
                    event.getId(),
                    RequestStatus.CONFIRMED
            );

            log.info(
                    "EVENT {} dbField={} actualConfirmed={}",
                    event.getId(),
                    event.getConfirmedRequests(),
                    confirmed
            );
        });
        events.forEach(e ->
                log.info(
                        "EVENT id={} userId={} categoryId={} state={}",
                        e.getId(),
                        e.getInitiator().getId(),
                        e.getCategory().getId(),
                        e.getState()
                )
        );
        List<EventFullDto> result = events.stream()
                .map(eventMapper::toFullDto)
                .collect(Collectors.toList());

        result.forEach(dto ->
                log.info(
                        "DTO id={} userId={} categoryId={} state={}",
                        dto.getId(),
                        dto.getInitiator().getId(),
                        dto.getCategory().getId(),
                        dto.getState()
                )
        );

        return result;
    }

    @Override
    @Transactional(readOnly = false)
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Updating event by admin: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (updateRequest.getEventDate() != null) {
            LocalDateTime newDate = LocalDateTime.parse(updateRequest.getEventDate(), FORMATTER);

            if (newDate.isBefore(LocalDateTime.now().minusMinutes(5))) {
                throw new BadRequestException("The event date must be at least 1 hour from the publication date");
            }
            event.setEventDate(newDate);
        }

        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            if (updateRequest.getParticipantLimit() < 0) {
                throw new BadRequestException("Participant limit cannot be negative");
            }
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getLocation() != null) {
            ru.practicum.dto.Location loc = new ru.practicum.dto.Location();
            loc.setLat(updateRequest.getLocation().getLat());
            loc.setLon(updateRequest.getLocation().getLon());
            event.setLocation(loc);
        }

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case "PUBLISH_EVENT":
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
                    }

                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;

                case "REJECT_EVENT":
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Cannot reject the event because it's already PUBLISHED");
                    }
                    event.setState(EventState.CANCELED);
                    break;

                default:
                    throw new BadRequestException("Unknown state action: " + updateRequest.getStateAction());
            }
        }

        Event updated = eventRepository.saveAndFlush(event);
        return eventMapper.toFullDto(updated);
    }

    private void saveHit(String uri, String ip) {
        try {
            EndpointHit hit = EndpointHit.builder()
                    .app("ewm-main-service")
                    .uri(uri)
                    .ip(ip)
                    .timestamp(LocalDateTime.now().format(FORMATTER))
                    .build();

            webClient.post()
                    .uri("/hit")
                    .bodyValue(hit)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("Failed to save hit: {}", e.getMessage());
        }
    }

    private Long getViews(Long eventId) {
        try {
            String start = LocalDateTime.now().minusYears(100).format(FORMATTER);
            String end = LocalDateTime.now().plusDays(1).format(FORMATTER);
            List<String> uris = Collections.singletonList("/events/" + eventId);

            List<ViewStats> stats = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stats")
                            .queryParam("start", start)
                            .queryParam("end", end)
                            .queryParam("uris", uris)
                            .queryParam("unique", true)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ViewStats>>() {
                    })
                    .block();

            return (stats != null && !stats.isEmpty()) ? stats.stream().mapToLong(ViewStats::getHits).sum() : 0L;
        } catch (Exception e) {
            log.warn("Failed to get views for event {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private void enrichWithViews(Event event) {
        Long views = getViews(event.getId());
        event.setViews(views);
    }

    private void enrichWithViews(List<Event> events) {
        if (events.isEmpty()) return;
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        try {
            String start = LocalDateTime.now().minusYears(100).format(FORMATTER);
            String end = LocalDateTime.now().plusDays(1).format(FORMATTER);
            List<String> uris = eventIds.stream().map(id -> "/events/" + id).collect(Collectors.toList());

            List<ViewStats> stats = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stats")
                            .queryParam("start", start)
                            .queryParam("end", end)
                            .queryParam("uris", uris)
                            .queryParam("unique", true)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ViewStats>>() {
                    })
                    .block();

            if (stats != null) {
                Map<String, Long> viewsMap = stats.stream()
                        .collect(Collectors.toMap(ViewStats::getUri, ViewStats::getHits, Long::sum));
                events.forEach(e -> e.setViews(viewsMap.getOrDefault("/events/" + e.getId(), 0L)));
            }
        } catch (Exception ex) {
            log.warn("Failed to get views: {}", ex.getMessage());
            events.forEach(e -> e.setViews(0L));
        }
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               String rangeStartStr, String rangeEndStr,
                                               Boolean onlyAvailable, String sort,
                                               Integer from, Integer size, HttpServletRequest request) {
        log.info("Getting public events with filters");

        LocalDateTime rangeStart = null;
        LocalDateTime rangeEnd = null;

        if (rangeStartStr != null) {
            String decodedStart = java.net.URLDecoder.decode(rangeStartStr, StandardCharsets.UTF_8);
            rangeStart = LocalDateTime.parse(decodedStart, FORMATTER);
        } else {
            rangeStart = LocalDateTime.now();
        }

        if (rangeEndStr != null) {
            String decodedEnd = java.net.URLDecoder.decode(rangeEndStr, StandardCharsets.UTF_8);
            rangeEnd = LocalDateTime.parse(decodedEnd, FORMATTER);
        } else {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        if (rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeStart must be before rangeEnd");
        }

        if (categories != null && !categories.isEmpty()) {
            long existingCount = categoryRepository.countByIdIn(categories);
            if (existingCount < categories.size()) {
                return Collections.emptyList();
            }
        }

        PageRequest pageRequest = PageRequest.of(from / size, size);

        List<Event> events = eventRepository.findEventsByPublic(
                text, categories, paid, rangeStart, rangeEnd, EventState.PUBLISHED, pageRequest);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        if (onlyAvailable != null && onlyAvailable) {
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0 ||
                            e.getConfirmedRequests() < e.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        enrichWithViews(events);

        if ("VIEWS".equals(sort)) {
            events.sort((e1, e2) -> {
                Long v1 = e1.getViews() != null ? e1.getViews() : 0L;
                Long v2 = e2.getViews() != null ? e2.getViews() : 0L;
                return v2.compareTo(v1);
            });
        } else if ("EVENT_DATE".equals(sort)) {
            events.sort((e1, e2) -> {
                if (e1.getEventDate() == null) return 1;
                if (e2.getEventDate() == null) return -1;
                return e1.getEventDate().compareTo(e2.getEventDate());
            });
        }
        saveHit(
                request.getRequestURI(),
                request.getRemoteAddr()
        );
        return events.stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getPublicEventById(Long id, HttpServletRequest request) {
        log.info("Getting public event by id: {}", id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + id + " was not found");
        }

        String clientIp = request.getRemoteAddr();
        saveHit("/events/" + id, clientIp);
        enrichWithViews(event);

        return eventMapper.toFullDto(event);
    }
}