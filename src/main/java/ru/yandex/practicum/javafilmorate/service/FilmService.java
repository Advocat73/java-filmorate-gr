package ru.yandex.practicum.javafilmorate.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.javafilmorate.model.*;
import ru.yandex.practicum.javafilmorate.storage.dao.FilmStorage;
import ru.yandex.practicum.javafilmorate.storage.dao.MarkStorage;
import ru.yandex.practicum.javafilmorate.utils.CheckUtil;
import ru.yandex.practicum.javafilmorate.utils.UnregisteredDataException;

import javax.validation.ConstraintViolationException;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    private final MarkStorage markStorage;
    private final EventService eventService;

    public Film findById(Integer filmId) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на получение фильма по id {}", filmId);
        return filmStorage.findById(filmId);
    }

    public Film addFilm(Film film) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на добавление фильма с id {}", film.getId());
        return filmStorage.addFilm(film);
    }

    public Film updateFilm(Film film) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на обновление фильма с id {}", film.getId());
        return filmStorage.updateFilm(film);
    }

    public List<Film> findAll() {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на получение списка фильмов");
        return filmStorage.findAll();
    }

    public void addMark(Integer filmId, Integer userId, Integer rating) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на добавление отметки \"mark\" " +
                "фильму с id {} от пользователя с id {} ", filmId, userId);
        try {
            markStorage.addMark(new Mark(filmId, userId, rating));
        } catch (ConstraintViolationException e) {
            throw new UnregisteredDataException("Пользователь с Id: " + userId + " уже дал оценку фильму с ID " + filmId);
        }
        eventService.add(new Event(EventType.LIKE, OperationType.ADD, filmId, userId));
    }

    public void deleteMark(Integer filmId, Integer userId) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на удаление отметки \"mark\" " +
                "фильму с id {} от пользователя с id {} ", filmId, userId);
        markStorage.deleteMark(filmId, userId);
        eventService.add(new Event(EventType.LIKE, OperationType.REMOVE, filmId, userId));
    }

    public List<Film> getPopularFilms(Integer limit) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на получение списка {} самых популярных фильмов", limit);
        return filmStorage.getPopularFilms(limit);
    }

    public void deleteFilm(int filmId) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на удаление фильма с Id={}.", filmId);
        CheckUtil.checkNotFound(filmStorage.deleteFilm(filmId), " фильм с Id=" + filmId);
    }

    public List<Film> commonFilms(int userId, int friendId) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на получение списка общих фильмов пользователя {} " +
                "и его друга {}.", userId, friendId);
        return filmStorage.commonFilms(userId, friendId);
    }

    public List<Film> getPopularByGenre(int count, int genreId) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на получение списка {} самых популярных фильмов в жанре " +
                "{}.", count, genreId);
        return filmStorage.getPopularByGenre(count, genreId);
    }

    public List<Film> getPopularByYear(int count, int year) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на получение списка {} самых популярных фильмов," +
                " выпущенных в {} году.", count, year);
        return filmStorage.getPopularByYear(count, year);
    }

    public List<Film> getPopularByGenreAndYear(int count, int genreId, int year) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на получение списка {} самых популярных фильмов в жанре " +
                "{}, выпущенных в {} году.", count, genreId, year);
        return filmStorage.getPopularByGenreAndYear(count, genreId, year);
    }

    public List<Film> searchBySubstring(String query, String by) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на получение списка всех фильмов, содержащих строку {}", query);
        return filmStorage.searchBySubstring(query, by);
    }

    public List<Film> findDirectorFilmsByYearOrLikes(int directorId, String sortBy) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на получение списка фильмов режиссера с id {}, " +
                "отсортированных по {}", directorId, sortBy);
        return filmStorage.findDirectorFilmsByYearOrLikes(directorId, sortBy);
    }
}
