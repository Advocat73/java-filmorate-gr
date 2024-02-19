package ru.yandex.practicum.javafilmorate.integrationTest;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.javafilmorate.JavaFilmorateApplication;
import ru.yandex.practicum.javafilmorate.model.Director;
import ru.yandex.practicum.javafilmorate.model.Film;
import ru.yandex.practicum.javafilmorate.model.Mpa;
import ru.yandex.practicum.javafilmorate.model.User;
import ru.yandex.practicum.javafilmorate.storage.dao.DirectorStorage;
import ru.yandex.practicum.javafilmorate.storage.dao.implementation.FilmDbStorage;
import ru.yandex.practicum.javafilmorate.storage.dao.implementation.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

@SpringBootTest(classes = JavaFilmorateApplication.class)
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
public class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userDbStorage;
    private final DirectorStorage directorStorage;

    private final Film film1 = new Film(null, "Film1", "Description1", LocalDate.parse("1970-01-01"),
            140, new Mpa(1, "G"));
    private final Film film2 = new Film(null, "Film2", "Description2", LocalDate.parse("1980-01-01"),
            90, new Mpa(2, "PG"));
    private final Film film3 = new Film(null, "Film3", "Description3", LocalDate.parse("1990-01-01"),
            190, new Mpa(2, "PG"));
    private final User firstUser = new User(1, "email@yandex.ru", "Login1", "Name1", LocalDate.parse("1970-01-01"), null);
    private final User secondUser = new User(1, "email@gmail.com", "Login2", "Name2", LocalDate.parse("1980-01-01"), null);
    private final User thirdUser = new User(3, "email@gmail.com", "Login3", "Name3", LocalDate.parse("1990-01-01"), null);
    private final Director director = new Director(1, "DirectorName");

    @BeforeEach
    void createFilmData() {
        filmStorage.addFilm(film1);
        filmStorage.addFilm(film2);
        filmStorage.addFilm(film3);

        userDbStorage.addUser(firstUser);
        userDbStorage.addUser(secondUser);
        userDbStorage.addUser(thirdUser);
    }

    @Test
    @DisplayName("Проверка метода update для Film")
    void testUpdateFilm() {
        Film updateFilm = new Film(1, "Film1", "updateDescription", LocalDate.parse("1990-01-01"), 140, new Mpa(1, "G"));
        filmStorage.updateFilm(updateFilm);
        Film afterUpdate = filmStorage.findById(1);
        Assertions.assertEquals(afterUpdate.getDescription(), "updateDescription");
    }

    @Test
    @DisplayName("Проверка метода findById для Film")
    void testFindFilmById() {
        Film film = filmStorage.findById(1);
        Assertions.assertEquals(film.getId(), 1);
    }

    @Test
    @DisplayName("Проверка метода findAll() для Film")
    void testFindAll() {
        List<Film> current = filmStorage.findAll();
        Assertions.assertEquals(3, current.size(), "Количество фильмов не совпадает");
    }

    @Test
    @DisplayName("Проверка метода deleteFilm")
    void testDeleteFilm() {
        filmStorage.deleteFilm(2);

        List<Film> current = filmStorage.findAll();
        Assertions.assertEquals(2, current.size(), "Количество film не совпадает.");

        Film[] expect = new Film[]{film1, film3};
        Assertions.assertArrayEquals(expect, current.toArray(), "Удален не тот film.");
    }

    @Test
    @DisplayName("Получение фильмов режиссёра, отсортированных по году")
    void testShouldFindDirectorFilmsByYear() {
        Director director2 = new Director(2, "DirectorName2");
        directorStorage.addDirector(director);
        directorStorage.addDirector(director2);
        //для каждого фильма указан режиссёр
        film1.getDirectors().add(director);
        filmStorage.updateFilm(film1);
        film2.getDirectors().add(director);
        filmStorage.updateFilm(film2);
        film3.getDirectors().add(director2);
        filmStorage.updateFilm(film3);
        //получение списка фильмов, отсортированного по году
        List<Film> filmsByYear = filmStorage.findDirectorFilmsByYearOrLikes(director.getId(), "year");
        Assertions.assertEquals(filmsByYear.size(), 2, "Количество фильмов не совпадает");
        Assertions.assertEquals(filmsByYear.get(0).getId(), film1.getId(), "Фильмы не отсортированы");
        Assertions.assertEquals(filmsByYear.get(1).getId(), film2.getId(), "Фильмы не отсортированы");
        Assertions.assertEquals(filmsByYear.get(0).getReleaseDate().toString(), "1970-01-01", "Даты не совпадают");
    }
}