package ru.yandex.practicum.javafilmorate.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.javafilmorate.model.*;
import ru.yandex.practicum.javafilmorate.storage.dao.FilmStorage;
import ru.yandex.practicum.javafilmorate.storage.dao.FriendStorage;
import ru.yandex.practicum.javafilmorate.storage.dao.MarkStorage;
import ru.yandex.practicum.javafilmorate.storage.dao.UserStorage;
import ru.yandex.practicum.javafilmorate.utils.CheckUtil;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class UserService {
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    private final FriendStorage friendStorage;
    private final EventService eventService;
    private final MarkStorage markStorage;

    public User addUser(User user) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на добавление пользователя с id {}", user.getId());
        return userStorage.addUser(user);
    }

    public User updateUser(User user) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на обновление пользователя с id {}", user.getId());
        return userStorage.updateUser(user);
    }

    public List<User> findAll() {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на получение списка всех пользователей");
        return userStorage.findAll();
    }

    public void addFriend(Integer userId, Integer friendId) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на добавление пользователю с id {} друга с id {}",
                userId, friendId);
        friendStorage.addFriend(userId, friendId);
        eventService.add(new Event(EventType.FRIEND, OperationType.ADD, friendId, userId));
    }

    public void deleteFriend(Integer userId, Integer friendId) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на удаление у пользователя с id {} друга с id {}",
                userId, friendId);
        friendStorage.deleteFriend(userId, friendId);
        eventService.add(new Event(EventType.FRIEND, OperationType.REMOVE, friendId, userId));
    }

    public List<User> getUserFriends(Integer userId) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на получение списка всех друзей пользователя с id {}", userId);
        return friendStorage.getUserFriends(userId);
    }

    public List<User> getCommonFriends(Integer userId, Integer friendId) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на получение списка общих друзей пользователей с id {} и id {}",
                userId, friendId);
        return friendStorage.getCommonsFriends(userId, friendId);
    }

    public User findById(Integer userId) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на получение пользователя по id {}", userId);
        return userStorage.findById(userId);
    }

    public void deleteUser(int userId) {
        log.info("СЕРВИС: Отправлен запрос к хранилищу на удаление у пользователя с id {}.", userId);
        CheckUtil.checkNotFound(userStorage.deleteUser(userId), " пользователь с id=" + userId);
    }

    public List<Film> findRecommendationsForUser(Integer requesterId) {
        log.info("СЕРВИС: Обработка запроса на рекомендации фильмов для пользователя с id {}", requesterId);

        Map<Integer, Set<Mark>> marks = markStorage.getAllMarks();
        List<Integer> userFilmsListId = new ArrayList<>();
        HashMap<Mark, Set<Mark>> mapMarksUserPresent = new HashMap<>();
        for (Map.Entry<Integer, Set<Mark>> filmMarks : marks.entrySet())
            for (Mark mark : filmMarks.getValue())
                if (Objects.equals(mark.getUserId(), requesterId)) {
                    userFilmsListId.add(filmMarks.getKey());
                    mapMarksUserPresent.put(mark, filmMarks.getValue());
                }

        /* мапа, состоящая из ID юзера-кандидата и суммарной разницы лайков основного юзера и юзера-кандидата */
        HashMap<Integer, Integer> diff = new HashMap<>();
        /* мапа, состоящая из ID юзера-кандидата и счетчика факта совпадения лайка основного юзера и юзера-кандидата */
        HashMap<Integer, Integer> freq = new HashMap<>();

        for (Map.Entry<Mark, Set<Mark>> setMarksUserPresent : mapMarksUserPresent.entrySet()) {
            Mark requesterMark = setMarksUserPresent.getKey();
            for (Mark candidatMark : setMarksUserPresent.getValue()) {
                if (!Objects.equals(requesterMark.getUserId(), candidatMark.getUserId())) {
                    if (!diff.containsKey(candidatMark.getUserId())) {
                        diff.put(candidatMark.getUserId(), 0);
                        freq.put(candidatMark.getUserId(), 0);
                    }
                    int oldGrade = diff.get(candidatMark.getUserId());
                    int oldCount = freq.get(candidatMark.getUserId());
                    int currentRatingDiff = requesterMark.getRating() - candidatMark.getRating();
                    diff.put(candidatMark.getUserId(), oldGrade + currentRatingDiff);
                    freq.put(candidatMark.getUserId(), oldCount + 1);
                }
            }
        }

        List<Integer> listResultSimilarId = diff.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Math.abs((double) e.getValue() / (double) freq.get(e.getKey()))))
                .entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())))
                .entrySet().stream().min(Comparator.comparingDouble(Map.Entry::getKey))
                .map(Map.Entry::getValue).orElse(List.of());

        /* Получаем и возвращаем список рекомендуемых фильмов*/
        return getListRecommendFilmsForUserFromListSimilarUserId(marks, userFilmsListId, listResultSimilarId);
    }

    private boolean isUserLikeForFilmGood(Integer filmId, Integer userId) {
        Film film = filmStorage.findById(filmId);
        Set<Mark> marks = film.getMarks();
        for (Mark mark : marks)
            if (mark.getUserId() == userId && mark.getRating() < 5)
                return false;
        return true;
    }

    private List<Film> getListRecommendFilmsForUserFromListSimilarUserId(Map<Integer, Set<Mark>> marks,
                                                                         List<Integer> userFilmsListId,
                                                                         List<Integer> listResultSimilarId) {
        /* Собираем фильмы в сет, чтобы повторяющиеся не попали в список */
        Set<Film> films = new HashSet<>();
        /* Для каждого ID из списка похожих юзеров формируем список Id фильмов, которые лайкнул похожий юзер */
        for (Integer similarUserId : listResultSimilarId) {
            List<Integer> similarUserFilmsListId = new ArrayList<>();
            for (Map.Entry<Integer, Set<Mark>> filmMarks : marks.entrySet())
                for (Mark like : filmMarks.getValue())
                    if (like.getUserId().equals(similarUserId))
                        similarUserFilmsListId.add(filmMarks.getKey());
            /* Удаляем повторяющиеся ID фильмов, проверяем на то,
            чтобы оценка похожего юзера была не ниже 5 и формируем список фильмов */
            similarUserFilmsListId.removeAll(userFilmsListId);
            similarUserFilmsListId.forEach(filmId -> {
                if (isUserLikeForFilmGood(filmId, similarUserId))
                    films.add(filmStorage.findById(filmId));
            });
        }
        return new ArrayList<>(films);
    }
}
