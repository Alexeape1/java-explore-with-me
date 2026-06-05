package ru.practicum.stats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.mapper.StatsMapper;
import ru.practicum.stats.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;
    private final StatsMapper statsMapper;

    @Override
    public void saveHit(EndpointHitDto hitDto) {
        log.info("Hit saved: {}", hitDto);
        statsRepository.save(statsMapper.toHit(hitDto));
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        log.info("Fetching stats from DB: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        if (unique) {
            return statsRepository.getUniqueStats(start, end, uris);
        } else {
            return statsRepository.getStats(start, end, uris);
        }
    }
}
