package ru.yandex.practicum.javafilmorate.storage.dao;

import ru.yandex.practicum.javafilmorate.model.Mark;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MarkStorage {
    void addMark(Mark like);

    void deleteMark(int filmId, int userId);

    List<Mark> getMarks(int filmId);

    Map<Integer, Set<Mark>> getAllMarks();
}