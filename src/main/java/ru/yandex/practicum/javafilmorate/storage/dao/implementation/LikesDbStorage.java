package ru.yandex.practicum.javafilmorate.storage.dao.implementation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.javafilmorate.model.Like;
import ru.yandex.practicum.javafilmorate.storage.dao.LikeStorage;
import ru.yandex.practicum.javafilmorate.utils.UnregisteredDataException;

import java.util.*;

@Slf4j
@AllArgsConstructor
@Repository
public class LikesDbStorage implements LikeStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void addLike(Like like) {
        int filmId = like.getFilmId();
        int userId = like.getUserId();
        isFilmRegistered(filmId);
        isUserRegistered(userId);
        log.info("ХРАНИЛИЩЕ: Сохранение отметки\"like\" фильму с id {} от пользователя с id {}", filmId, userId);
        String sqlQuery = "INSERT INTO LIKES (FILM_ID, USER_ID, GRADE) VALUES (?, ?, ?)";
        jdbcTemplate.update(sqlQuery, filmId, userId, like.getGrade());
    }

    @Override
    public void deleteLike(int filmId, int userId) {
        isFilmRegistered(filmId);
        isUserRegistered(userId);
        log.info("ХРАНИЛИЩЕ: Удаление отметки\"like\" у фильма с id {} от пользователя с id {}", filmId, userId);
        String sqlQuery = "DELETE FROM LIKES WHERE FILM_ID = ? AND USER_ID = ?";
        jdbcTemplate.update(sqlQuery, filmId, userId);
    }

    @Override
    public List<Like> getLikes(int filmId) {
        isFilmRegistered(filmId);
        log.info("ХРАНИЛИЩЕ: Получение отметок \"like\" для фильма с id {}", filmId);
        String sqlQuery = "SELECT * FROM LIKES WHERE FILM_ID = ?";
        return jdbcTemplate.query(sqlQuery,
                (rs, rowNum) -> new Like(filmId, rs.getInt("USER_ID"), rs.getInt("GRADE")),
                filmId);
    }

    @Override
    public Map<Integer, Set<Like>> getAllLikes() {
        log.info("ХРАНИЛИЩЕ: Получение карты всех лайков");
        List<Map<String, Object>> likesDatabaseResult = jdbcTemplate.queryForList("SELECT * from likes");
        Map<Integer, Set<Like>> likes = new HashMap<>();
        for (Map<String, Object> map : likesDatabaseResult)
            likes.computeIfAbsent((Integer) map.get("film_id"), k -> new HashSet<>())
                    .add(new Like((Integer) map.get("FILM_ID"), (Integer) map.get("USER_ID"), (Integer) map.get("GRADE")));
        return likes;
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
