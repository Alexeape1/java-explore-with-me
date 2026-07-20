package ru.practicum.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.LocationDto;
import ru.practicum.dto.NewLocationDto;
import ru.practicum.service.LocationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/locations")
public class AdminLocationController {

   private final LocationService locationService;

   @PostMapping
   @ResponseStatus(HttpStatus.CREATED)
    public LocationDto create(@Valid @RequestBody NewLocationDto newLocationDto) {
        return locationService.create(newLocationDto);
    }

    @GetMapping
    public List<LocationDto> getAll() {
       return locationService.getAll();
    }
}
