package ru.practicum.service;

import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.LocationDto;
import ru.practicum.dto.NewLocationDto;

import java.util.List;

public interface LocationService {

    LocationDto create(NewLocationDto newLocationDto);

    List<LocationDto> getAll();

    List<EventShortDto> getEventsInLocation(Long locationId);
}