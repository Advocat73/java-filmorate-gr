package ru.yandex.practicum.javafilmorate.storage.dao;

import ru.yandex.practicum.javafilmorate.model.Film;
import ru.yandex.practicum.javafilmorate.model.Genre;

import java.util.List;
import java.util.Map;

public interface GenreStorage {

    Genre findById(int genreId);

    List<Genre> findAll();

    Map<Integer, List<Genre>> getFilmsWithGenres();

    void reloadGenres(Film film);

    void deleteFilmGenre(Film film);

    void addFilmGenre(Film film);
}
