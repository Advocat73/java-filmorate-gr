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

        Map<Integer, Set<Mark>> allMarksMap = markStorage.getAllMarks();

        List<Integer> requesterFilmsListId = new ArrayList<>();
        HashMap<Mark, Set<Mark>> marksRequesterFilmsListIdPresentMap = new HashMap<>();

        for (Map.Entry<Integer, Set<Mark>> filmMarks : allMarksMap.entrySet())
            for (Mark mark : filmMarks.getValue())
                if (Objects.equals(mark.getUserId(), requesterId)) {
                    requesterFilmsListId.add(filmMarks.getKey());
                    marksRequesterFilmsListIdPresentMap.put(mark, filmMarks.getValue());
                }

        return getListRecommendFilmsForRequesterFromListSimilarUserId(allMarksMap, requesterFilmsListId,
                getListSimilarUsers(marksRequesterFilmsListIdPresentMap));
    }

    private List<Integer> getListSimilarUsers(HashMap<Mark, Set<Mark>> marksRequesterFilmsListIdPresentMap) {
        HashMap<Integer, Integer> candidateAndRequesterSumDiffMap = new HashMap<>();
        HashMap<Integer, Integer> counterFreqConcurrenceMarksMap = new HashMap<>();
        computeSummaMarksDiff(marksRequesterFilmsListIdPresentMap
                , candidateAndRequesterSumDiffMap
                , counterFreqConcurrenceMarksMap);

        return candidateAndRequesterSumDiffMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Math.abs((double) e.getValue()
                        / (double) counterFreqConcurrenceMarksMap.get(e.getKey()))))
                .entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())))
                .entrySet().stream().min(Comparator.comparingDouble(Map.Entry::getKey))
                .map(Map.Entry::getValue).orElse(List.of());
    }

    private void computeSummaMarksDiff(HashMap<Mark, Set<Mark>> marksRequesterFilmsListIdPresentMap,
                                       HashMap<Integer, Integer> candidateAndRequesterSumDiffMap,
                                       HashMap<Integer, Integer> counterFreqConcurrenceMarksMap) {
        for (Map.Entry<Mark, Set<Mark>> MarksRequesterPresentEntry : marksRequesterFilmsListIdPresentMap.entrySet()) {
            Mark requesterMark = MarksRequesterPresentEntry.getKey();
            for (Mark candidatMark : MarksRequesterPresentEntry.getValue()) {
                if (!Objects.equals(requesterMark.getUserId(), candidatMark.getUserId())) {
                    if (!candidateAndRequesterSumDiffMap.containsKey(candidatMark.getUserId())) {
                        candidateAndRequesterSumDiffMap.put(candidatMark.getUserId(), 0);
                        counterFreqConcurrenceMarksMap.put(candidatMark.getUserId(), 0);
                    }
                    int oldGrade = candidateAndRequesterSumDiffMap.get(candidatMark.getUserId());
                    int oldCount = counterFreqConcurrenceMarksMap.get(candidatMark.getUserId());
                    int currentRatingDiff = requesterMark.getRating() - candidatMark.getRating();
                    candidateAndRequesterSumDiffMap.put(candidatMark.getUserId(), oldGrade + currentRatingDiff);
                    counterFreqConcurrenceMarksMap.put(candidatMark.getUserId(), oldCount + 1);
                }
            }
        }
    }

    private List<Film> getListRecommendFilmsForRequesterFromListSimilarUserId(Map<Integer, Set<Mark>> allMarksMap,
                                                                              List<Integer> userFilmsListId,
                                                                              List<Integer> listResultSimilarId) {
        ArrayList<Mark> marks = new ArrayList<>();
        for (Set<Mark> ms : allMarksMap.values())
            marks.addAll(ms);

        return marks.stream().filter(ms -> listResultSimilarId.contains(ms.getUserId()))
                .filter(ms -> userFilmsListId.contains(ms.getUserId()))
                .filter(ms -> ms.getRating() > 5)
                .map(Mark::getFilmId)
                .map(filmStorage::findById)
                .collect(Collectors.toList());
    }
}
