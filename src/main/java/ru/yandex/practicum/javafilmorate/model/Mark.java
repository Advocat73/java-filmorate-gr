package ru.yandex.practicum.javafilmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class Mark {
    @NotNull
    final Integer filmId;
    @NotNull
    final Integer userId;
    @Min(1)
    @Max(10)
    @NotNull
    Integer rating;
}
