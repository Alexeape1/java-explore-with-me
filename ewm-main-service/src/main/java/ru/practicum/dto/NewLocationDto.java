package ru.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NewLocationDto {

    @NotBlank
    private String name;

    @NotNull
    private Double lat;

    @NotNull
    private Double lon;

    @NotNull
    private Double radius;
}