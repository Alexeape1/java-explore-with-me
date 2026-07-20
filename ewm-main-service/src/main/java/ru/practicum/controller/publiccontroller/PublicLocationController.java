package ru.practicum.controller.publiccontroller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.EventShortDto;
import ru.practicum.service.LocationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/locations")
public class PublicLocationController {

    private final LocationService locationService;

    @GetMapping("/{locationId}/events")
    public List<EventShortDto> getEventsInLocation(@PathVariable Long locationId) {
        return locationService.getEventsInLocation(locationId);
    }
}
