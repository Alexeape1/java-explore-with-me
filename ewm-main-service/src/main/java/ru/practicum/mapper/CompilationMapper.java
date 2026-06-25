package ru.practicum.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CompilationMapper {

    private final EventMapper eventMapper;

    public CompilationDto toDto(Compilation compilation) {
        if (compilation == null) return null;

        List<EventShortDto> eventDto = compilation.getEvents() != null ?
                compilation.getEvents().stream()
                        .map(eventMapper::toShortDto)
                        .collect(Collectors.toList()) :
                new ArrayList<>();

        return CompilationDto.builder()
                .id(compilation.getId())
                .events(eventDto)
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }

    public Compilation toEntity(NewCompilationDto newCompilationDto, List<Event> events) {
        if (newCompilationDto == null) return null;

        Compilation compilation = new Compilation();
        compilation.setTitle(newCompilationDto.getTitle());
        compilation.setPinned(newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false);
        compilation.setEvents(events != null ? events : new ArrayList<>());

        return compilation;
    }
}