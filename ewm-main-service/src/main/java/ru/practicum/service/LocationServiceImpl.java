package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.LocationDto;
import ru.practicum.dto.NewLocationDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.ManagedLocation;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.LocationRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final EventMapper eventMapper;
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;

    @Override
    public LocationDto create(NewLocationDto newLocationDto) {

        log.info("Create new location: {}", newLocationDto);

        ManagedLocation location =
                ManagedLocation.builder()
                        .name(newLocationDto.getName())
                        .lat(newLocationDto.getLat())
                        .lon(newLocationDto.getLon())
                        .radius(newLocationDto.getRadius())
                        .build();

        location = locationRepository.save(location);

        return LocationDto.builder()
                .id(location.getId())
                .name(location.getName())
                .lat(location.getLat())
                .lon(location.getLon())
                .radius(location.getRadius())
                .build();
    }

    @Override
    public List<LocationDto> getAll() {
        log.info("Get all locations");

        return locationRepository.findAll()
                .stream()
                .map(l -> LocationDto.builder()
                        .id(l.getId())
                        .name(l.getName())
                        .lat(l.getLat())
                        .lon(l.getLon())
                        .radius(l.getRadius())
                        .build())
                .toList();
    }

    @Override
    public List<EventShortDto> getEventsInLocation(Long locationId) {
        log.info("Get events in location:{}", locationId);

        ManagedLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("location" + locationId + " is not found"));

        return eventRepository.findEventsInRadius(location.getLat(),
                        (location.getLon()),
                        (location.getRadius()))
                .stream()
                .map(eventMapper::toShortDto)
                .toList();
    }
}
