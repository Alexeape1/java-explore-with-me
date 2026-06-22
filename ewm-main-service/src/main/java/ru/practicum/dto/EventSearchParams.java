package ru.practicum.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventSearchParams {

    private String text;
    private List<Long> categories;
    private Boolean paid;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rangeStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rangeEnd;
    private Boolean onlyAvailable;
    private String sort;
    private Integer from = 0;
    private Integer size = 10;

    @AssertTrue(message = "rangeEnd must be after rangeStart")
    public boolean isRangeValid() {
        if (rangeStart == null || rangeEnd == null) {
            return true; // если одна из дат не передана, пропускаем проверку
        }
        return rangeEnd.isAfter(rangeStart);
    }
}