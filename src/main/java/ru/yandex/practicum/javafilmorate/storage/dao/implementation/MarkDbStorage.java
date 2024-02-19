package ru.yandex.practicum.javafilmorate.storage.dao.implementation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.javafilmorate.model.Mark;
import ru.yandex.practicum.javafilmorate.storage.dao.MarkStorage;
import ru.yandex.practicum.javafilmorate.utils.UnregisteredDataException;

import java.util.*;

@Slf4j
@AllArgsConstructor
@Repository
public class MarkDbStorage implements MarkStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void addMark(Mark mark) {
        int filmId = mark.getFilmId();
        int userId = mark.getUserId();
        isFilmRegistered(filmId);
        isUserRegistered(userId);
        log.info("ХРАНИЛИЩЕ: Сохранение отметки\"mark\" фильму с id {} от пользователя с id {}", filmId, userId);
        String sqlQuery = "INSERT INTO MARKS (FILM_ID, USER_ID, RATING) VALUES (?, ?, ?)";
        jdbcTemplate.update(sqlQuery, filmId, userId, mark.getRating());
    }

    @Override
    public void deleteMark(int filmId, int userId) {
        isFilmRegistered(filmId);
        isUserRegistered(userId);
        log.info("ХРАНИЛИЩЕ: Удаление отметки\"mark\" у фильма с id {} от пользователя с id {}", filmId, userId);
        String sqlQuery = "DELETE FROM MARKS WHERE FILM_ID = ? AND USER_ID = ?";
        jdbcTemplate.update(sqlQuery, filmId, userId);
    }

    @Override
    public List<Mark> getMarks(int filmId) {
        isFilmRegistered(filmId);
        log.info("ХРАНИЛИЩЕ: Получение отметок \"mark\" для фильма с id {}", filmId);
        String sqlQuery = "SELECT * FROM MARKS WHERE FILM_ID = ?";
        return jdbcTemplate.query(sqlQuery,
                (rs, rowNum) -> new Mark(filmId, rs.getInt("USER_ID"), rs.getInt("RATING")),
                filmId);
    }

    @Override
    public Map<Integer, Set<Mark>> getAllMarks() {
        log.info("ХРАНИЛИЩЕ: Получение карты всех оценок");
        List<Map<String, Object>> marksDatabaseResult = jdbcTemplate.queryForList("SELECT * FROM MARKS");
        Map<Integer, Set<Mark>> marks = new HashMap<>();
        for (Map<String, Object> map : marksDatabaseResult)
            marks.computeIfAbsent((Integer) map.get("film_id"), k -> new HashSet<>())
                    .add(new Mark((Integer) map.get("FILM_ID"), (Integer) map.get("USER_ID"), (Integer) map.get("RATING")));
        return marks;
    }

    private void isFilmRegistered(int filmId) {
        log.info("ХРАНИЛИЩЕ: Проверка регистрации фильма с {} в системе", filmId);
        String sqlQuery = "SELECT * FROM FILMS WHERE FILM_ID = ?";
        SqlRowSet filmRow = jdbcTemplate.queryForRowSet(sqlQuery, filmId);
        if (!filmRow.next()) {
            throw new UnregisteredDataException("Фильм с id " + filmId + " не зарегистрирован в системе");
        }
    }

    private void isUserRegistered(int userId) {
        log.info("ХРАНИЛИЩЕ: Проверка регистрации пользователя с id {} в системе", userId);
        String sqlQuery = "SELECT * FROM USERS WHERE USER_ID = ?";
        SqlRowSet userRow = jdbcTemplate.queryForRowSet(sqlQuery, userId);
        if (!userRow.next()) {
            throw new UnregisteredDataException("Пользователь с id " + userId + " не зарегистрирован в системе");
        }
    }
}
