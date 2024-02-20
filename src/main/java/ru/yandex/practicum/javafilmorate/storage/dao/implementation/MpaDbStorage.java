package ru.yandex.practicum.javafilmorate.storage.dao.implementation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.javafilmorate.model.Mpa;
import ru.yandex.practicum.javafilmorate.storage.dao.MpaStorage;
import ru.yandex.practicum.javafilmorate.utils.UnregisteredDataException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class MpaDbStorage implements MpaStorage {
    private final JdbcTemplate jdbcTemplate;
    private final List<Mpa> mpaList;

    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        mpaList = jdbcTemplate.query("SELECT * FROM MPA", (rs, rowNum) ->
                new Mpa(rs.getInt("MPA_ID"), rs.getString("MPA_NAME")));
    }

    @Override
    public Mpa findById(int mpaId) {
        log.info("ХРАНИЛИЩЕ: Получение рейтинга по id {}", mpaId);
        String sqlQuery = "SELECT * FROM MPA WHERE MPA_ID = ?";
        SqlRowSet rs = jdbcTemplate.queryForRowSet(sqlQuery, mpaId);
        if (rs.next()) {
            return mpaRowMap(rs);
        } else {
            throw new UnregisteredDataException("MPA с id " + mpaId + " не зарегистрирован в системе");
        }
    }

    @Override
    public List<Mpa> findAll() {
        log.info("ХРАНИЛИЩЕ: Получение списа всех рейтингов");
        List<Mpa> mpaList = new ArrayList<>();
        String sqlQuery = "SELECT * FROM MPA";
        SqlRowSet rs = jdbcTemplate.queryForRowSet(sqlQuery);
        while (rs.next()) {
            mpaList.add(mpaRowMap(rs));
        }
        return mpaList;
    }

    @Override
    public Mpa getMpaRating(int mpaId) {
        for (Mpa mpaTmp : mpaList)
            if (mpaTmp.getId() == mpaId)
                return new Mpa(mpaTmp.getId(), mpaTmp.getName());
        return null;
    }

    private Mpa mpaRowMap(SqlRowSet rs) {
        log.info("ХРАНИЛИЩЕ: Производится маппинг MPA");
        return new Mpa(
                rs.getInt("MPA_ID"),
                rs.getString("MPA_NAME")
        );
    }
}
