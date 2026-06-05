package ru.practicum.stats.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.model.EndpointHit;

@Component
public class StatsMapper {

    public EndpointHit toHit(EndpointHitDto hitDto) {
        return EndpointHit.builder()
                .app(hitDto.getApp())
                .uri(hitDto.getUri())
                .ip(hitDto.getIp())
                .timestamp(hitDto.getTimestamp())
                .build();
    }

    public EndpointHitDto toHitDto(EndpointHit hit) {
        if (hit == null) {
            return null;
        }

        return EndpointHitDto.builder()
                .app(hit.getApp())
                .uri(hit.getUri())
                .ip(hit.getIp())
                .timestamp(hit.getTimestamp())
                .build();
    }
}
