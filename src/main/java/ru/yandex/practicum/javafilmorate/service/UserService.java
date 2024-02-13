package ru.yandex.practicum.javafilmorate.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.javafilmorate.model.*;
import ru.yandex.practicum.javafilmorate.storage.dao.FilmStorage;
import ru.yandex.practicum.javafilmorate.storage.dao.FriendStorage;
import ru.yandex.practicum.javafilmorate.storage.dao.LikeStorage;
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
    private final LikeStorage likeStorage;

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

    public List<Film> findRecommendationsForUser(Integer userId) {
        log.info("СЕРВИС: Обработка запроса на рекомендации фильмов для пользователя с id {}", userId);
        /* Получаем матрицу всех лайков: каждый элемент содержит: Id фильма + список лайков этому фильму */
        Map<Integer, Set<Like>> likes = likeStorage.getAllLikes();
        /* Формируем: */
        /* список Id фильмов, которые лайкнул юзер и */
        /* мапу, состоящую из лайка юзера и списка лайков, где присутствует юзер с лайком, который является ключом */
        List<Integer> userFilmsListId = new ArrayList<>();
        HashMap<Like, Set<Like>> mapLikesUserPresent = new HashMap<>();
        for (Map.Entry<Integer, Set<Like>> filmLikes : likes.entrySet())
            for (Like like : filmLikes.getValue())
                if (Objects.equals(like.getUserId(), userId)) {
                    userFilmsListId.add(filmLikes.getKey());
                    mapLikesUserPresent.put(like, filmLikes.getValue());
                }
        /* Создаем: */
        /* мапу, состоящую из ID юзера-кандидата и суммарной разницы лайков основного юзера и юзера-кандидата */
        HashMap<Integer, Integer> diff = new HashMap<>();
        /* мапу, состоящую из ID юзера-кандидата и счетчика факта совпадения лайка основного юзера и юзера-кандидата */
        HashMap<Integer, Integer> freq = new HashMap<>();
        /* Проходим по всем спискам лайков, где есть лайк основного юзера */
        for (Map.Entry<Like, Set<Like>> setLikesUserPresent : mapLikesUserPresent.entrySet()) {
            /* Получаем лайк основного юзера, с которым будем сранивать всех кандидатов */
            Like userLike = setLikesUserPresent.getKey();
            /* Получаем очередной лайк из очередного списка */
            for (Like candidatLike : setLikesUserPresent.getValue()) {
                /* Если базовый лайк не совпадает с кандидатом */
                if (!Objects.equals(userLike.getUserId(), candidatLike.getUserId())) {
                    /* Если кандидат появился впервые -> создадим графу в мапах */
                    if (!diff.containsKey(candidatLike.getUserId())) {
                        diff.put(candidatLike.getUserId(), 0);
                        freq.put(candidatLike.getUserId(), 0);
                    }
                    /* Возьмем данные из графы соответствующей мапы, где записаны данные про кандидата */
                    int oldGrade = diff.get(candidatLike.getUserId());
                    int oldCount = freq.get(candidatLike.getUserId());
                    /* Получаем разницу в оценке фильма основного юзера и кандидата и запишем новые данные в мапы */
                    int currentGradeDiff = userLike.getGrade() - candidatLike.getGrade();
                    diff.put(candidatLike.getUserId(), oldGrade + currentGradeDiff);
                    freq.put(candidatLike.getUserId(), oldCount + 1);
                }
            }
        }
        /* Запоняем список айтемов, каждый из которых содержит ID кандидата и (разницу в оценках фильма)/(количество совпадений) */
        List<ResultItem> listResult = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : diff.entrySet()) {
            double res = (double) e.getValue() / (double) freq.get(e.getKey());
            listResult.add(new ResultItem(e.getKey(), Math.abs(res)));
        }
        /* Сортируем от меньшего к большему и получаем из списка айтемов список айдишников похожих юзеров, */
        /* сравнивая по полученным оценкам с первым. Останутся только те, которые равны первому */
        List<Integer> listResultSimilarId = sortListResulItemAndReturnListId(listResult);
        /* Получаем и возвращаем список рекомендуемых фильмов*/
        return getListRecommendFilmsForUserFromListSimilarUserId(likes, userFilmsListId, listResultSimilarId);
    }

    private boolean isUserLikeForFilmGood(Integer filmId, Integer userId) {
        Film film = filmStorage.findById(filmId);
        Set<Like> likes = film.getLikes();
        for (Like like : likes)
            if (like.getUserId() == userId && like.getGrade() < 5)
                return false;
        return true;
    }

    private List<Integer> sortListResulItemAndReturnListId(List<ResultItem> listResult) {
        /* Сортируем лист результатов */
        listResult.sort(Comparator.comparingDouble(ResultItem::getFinalGrade));
        /* Запоминаем первый - с самой маленькой оценкой-разницей с основным юзером */
        Double resD = listResult.get(0).getFinalGrade();
        List<Integer> returnList = new ArrayList<>();
        /* Смотрим есть ли еще юзеры с такой же оценкой-разницей, если есть включаем в возвращаемый список */
        int i = 0;
        while (listResult.get(i).getFinalGrade().equals(resD))
            returnList.add(listResult.get(i++).getUserId());
        return returnList;
    }

    private List<Film> getListRecommendFilmsForUserFromListSimilarUserId(Map<Integer, Set<Like>> likes,
                                                                         List<Integer> userFilmsListId,
                                                                         List<Integer> listResultSimilarId) {
        /* Собираем фильмы в сет, чтобы повторяющиеся не попали в список */
        Set<Film> films = new HashSet<>();
        /* Для каждого ID из списка похожих юзеров формируем список Id фильмов, которые лайкнул похожий юзер */
        for (Integer similarUserId : listResultSimilarId) {
            List<Integer> similarUserFilmsListId = new ArrayList<>();
            for (Map.Entry<Integer, Set<Like>> filmLikes : likes.entrySet())
                for (Like like : filmLikes.getValue())
                    if (like.getUserId().equals(similarUserId))
                        similarUserFilmsListId.add(filmLikes.getKey());
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

    private static class ResultItem {
        Integer userId;
        Double finalGrade;

        public ResultItem(Integer userId, Double finalGrade) {
            this.userId = userId;
            this.finalGrade = finalGrade;
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        public Double getFinalGrade() {
            return finalGrade;
        }

        public void setFinalGrade(Double finalGrade) {
            this.finalGrade = finalGrade;
        }
    }
}
