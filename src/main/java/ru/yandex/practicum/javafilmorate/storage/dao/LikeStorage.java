package ru.yandex.practicum.javafilmorate.storage.dao;

import ru.yandex.practicum.javafilmorate.model.Like;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LikeStorage {
    void addLike(Like like);

    void deleteLike(int filmId, int userId);

    List<Like> getLikes(int filmId);

    Map<Integer, Set<Like>> getAllLikes();
}