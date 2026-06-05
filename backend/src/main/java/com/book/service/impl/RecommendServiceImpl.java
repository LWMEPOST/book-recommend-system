package com.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.book.common.Constants;
import com.book.common.Result;
import com.book.entity.Book;
import com.book.entity.BorrowRecord;
import com.book.entity.Category;
import com.book.entity.Favorite;
import com.book.mapper.BookMapper;
import com.book.mapper.BorrowRecordMapper;
import com.book.mapper.CategoryMapper;
import com.book.mapper.FavoriteMapper;
import com.book.mapper.UserMapper;
import com.book.service.AlgorithmConfigService;
import com.book.service.RatingService;
import com.book.service.RecommendService;
import com.book.vo.BookVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    private static final Logger log = LoggerFactory.getLogger(RecommendServiceImpl.class);

    private final RatingService ratingService;
    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final FavoriteMapper favoriteMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final UserMapper userMapper;
    private final AlgorithmConfigService algorithmConfigService;

    private final Map<Long, List<BookVO>> personalRecommendCache = new ConcurrentHashMap<>();
    private final Map<Long, List<BookVO>> categoryRecommendCache = new ConcurrentHashMap<>();
    private volatile List<BookVO> hotRecommendCache;
    private volatile LocalDateTime lastRefreshTime;

    public RecommendServiceImpl(RatingService ratingService, BookMapper bookMapper,
                                CategoryMapper categoryMapper, FavoriteMapper favoriteMapper,
                                BorrowRecordMapper borrowRecordMapper, UserMapper userMapper,
                                AlgorithmConfigService algorithmConfigService) {
        this.ratingService = ratingService;
        this.bookMapper = bookMapper;
        this.categoryMapper = categoryMapper;
        this.favoriteMapper = favoriteMapper;
        this.borrowRecordMapper = borrowRecordMapper;
        this.userMapper = userMapper;
        this.algorithmConfigService = algorithmConfigService;
    }

    @Override
    public Result<List<BookVO>> getPersonalRecommend(Long userId) {
        if (userId == null) {
            return getHotRecommend();
        }
        List<BookVO> result = personalRecommendCache.computeIfAbsent(userId, this::calculatePersonalRecommend);
        return Result.success(result);
    }

    @Override
    public Result<List<BookVO>> getHotRecommend() {
        return Result.success(getHotRecommendBooks());
    }

    @Override
    public Result<List<BookVO>> getLatestRecommend() {
        return Result.success(calculateLatestRecommend());
    }

    @Override
    public Result<List<BookVO>> getCategoryRecommend(Long userId) {
        if (userId == null) {
            return getHotRecommend();
        }
        List<BookVO> result = categoryRecommendCache.computeIfAbsent(userId, this::calculateCategoryRecommend);
        return Result.success(result);
    }

    @Override
    public Result<Map<String, Object>> refreshRecommendCache() {
        Map<String, Object> summary = refreshRecommendCacheInternal();
        return Result.success("推荐缓存已刷新", summary);
    }

    @Override
    public Result<Map<String, Object>> getRecommendSummary() {
        Map<Long, Map<Long, Double>> ratingMatrix = buildRatingMatrix(ratingService.getAllRatings());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalUserCount", userMapper.selectCount(null));
        summary.put("activeRecommendUserCount", ratingMatrix.size());
        summary.put("totalBookCount", bookMapper.selectCount(null));
        summary.put("onlineBookCount", bookMapper.selectCount(new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, Constants.BOOK_STATUS_ON)));
        summary.put("ratingCount", ratingService.count());
        summary.put("favoriteCount", favoriteMapper.selectCount(null));
        summary.put("borrowCount", borrowRecordMapper.selectCount(null));
        summary.put("configCount", algorithmConfigService.count());
        summary.put("hotCacheCount", hotRecommendCache != null ? hotRecommendCache.size() : 0);
        summary.put("personalCacheCount", personalRecommendCache.size());
        summary.put("categoryCacheCount", categoryRecommendCache.size());
        summary.put("cacheReady", hotRecommendCache != null || !personalRecommendCache.isEmpty() || !categoryRecommendCache.isEmpty());
        summary.put("lastRefreshTime", lastRefreshTime);
        return Result.success(summary);
    }

    @Override
    public void evictAllRecommendCache() {
        personalRecommendCache.clear();
        categoryRecommendCache.clear();
        hotRecommendCache = null;
    }

    @Scheduled(cron = "${app.recommend-cache.refresh-cron:0 0 3 * * ?}", zone = "${app.recommend-cache.refresh-zone:Asia/Shanghai}")
    public void scheduledRefreshRecommendCache() {
        Map<String, Object> summary = refreshRecommendCacheInternal();
        log.info("Refreshed recommendation cache on schedule: {}", summary);
    }

    private synchronized Map<String, Object> refreshRecommendCacheInternal() {
        Map<Long, Map<Long, Double>> ratingMatrix = buildRatingMatrix(ratingService.getAllRatings());
        List<Long> activeUserIds = new ArrayList<>(ratingMatrix.keySet());

        List<BookVO> hotBooks = calculateHotRecommend();
        hotRecommendCache = hotBooks;
        Map<Long, List<BookVO>> refreshedPersonalCache = new HashMap<>();
        Map<Long, List<BookVO>> refreshedCategoryCache = new HashMap<>();

        for (Long userId : activeUserIds) {
            refreshedPersonalCache.put(userId, calculatePersonalRecommend(userId, ratingMatrix));
            refreshedCategoryCache.put(userId, calculateCategoryRecommend(userId));
        }

        personalRecommendCache.clear();
        personalRecommendCache.putAll(refreshedPersonalCache);
        categoryRecommendCache.clear();
        categoryRecommendCache.putAll(refreshedCategoryCache);
        lastRefreshTime = LocalDateTime.now();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("refreshTime", lastRefreshTime);
        summary.put("activeUserCount", activeUserIds.size());
        summary.put("hotBookCount", hotBooks.size());
        summary.put("personalCacheCount", personalRecommendCache.size());
        summary.put("categoryCacheCount", categoryRecommendCache.size());
        return summary;
    }

    private List<BookVO> calculatePersonalRecommend(Long userId) {
        return calculatePersonalRecommend(userId, buildRatingMatrix(ratingService.getAllRatings()));
    }

    private List<BookVO> calculatePersonalRecommend(Long userId, Map<Long, Map<Long, Double>> userBookRatings) {
        Map<Long, Double> targetRatings = userBookRatings.get(userId);
        if (targetRatings == null || targetRatings.size() < getColdStartMinRatings()) {
            return filterHotRecommend(targetRatings != null ? targetRatings.keySet() : Collections.emptySet());
        }

        double targetAvg = targetRatings.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Map<Long, Double> userSimilarities = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : userBookRatings.entrySet()) {
            if (entry.getKey().equals(userId)) {
                continue;
            }
            double similarity = pearsonSimilarity(targetRatings, entry.getValue());
            if (similarity > getSimilarityThreshold()) {
                userSimilarities.put(entry.getKey(), similarity);
            }
        }

        if (userSimilarities.isEmpty()) {
            return filterHotRecommend(targetRatings.keySet());
        }

        List<Map.Entry<Long, Double>> sortedSimilarUsers = userSimilarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(getSimilarUserCount())
                .collect(Collectors.toList());

        Set<Long> interactedBookIds = targetRatings.keySet();
        Map<Long, Double> predictedScores = new HashMap<>();
        Map<Long, Double> similaritySums = new HashMap<>();

        for (Map.Entry<Long, Double> similarUserEntry : sortedSimilarUsers) {
            Long similarUserId = similarUserEntry.getKey();
            double similarity = similarUserEntry.getValue();
            Map<Long, Double> similarUserRatings = userBookRatings.get(similarUserId);
            double similarUserAvg = similarUserRatings.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            for (Map.Entry<Long, Double> bookRating : similarUserRatings.entrySet()) {
                Long bookId = bookRating.getKey();
                if (interactedBookIds.contains(bookId)) {
                    continue;
                }

                double weightedScore = similarity * (bookRating.getValue() - similarUserAvg);
                predictedScores.merge(bookId, weightedScore, Double::sum);
                similaritySums.merge(bookId, Math.abs(similarity), Double::sum);
            }
        }

        Map<Long, Double> finalScores = new HashMap<>();
        for (Map.Entry<Long, Double> entry : predictedScores.entrySet()) {
            double similaritySum = similaritySums.getOrDefault(entry.getKey(), 0.0);
            if (similaritySum <= 0) {
                continue;
            }
            finalScores.put(entry.getKey(), targetAvg + entry.getValue() / similaritySum);
        }

        if (finalScores.isEmpty()) {
            return filterHotRecommend(interactedBookIds);
        }

        List<Long> recommendedBookIds = finalScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return finalizeRecommendList(recommendedBookIds, interactedBookIds);
    }

    private List<BookVO> calculateHotRecommend() {
        List<Book> books = bookMapper.selectList(new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, Constants.BOOK_STATUS_ON));
        if (books.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> borrowCountMap = borrowRecordMapper.selectList(new LambdaQueryWrapper<BorrowRecord>())
                .stream()
                .collect(Collectors.groupingBy(BorrowRecord::getBookId, Collectors.counting()));

        List<Book> rankedBooks = books.stream()
                .sorted(Comparator
                        .comparingDouble(this::getBookAvgRating).reversed()
                        .thenComparing(Comparator.comparingLong((Book book) -> borrowCountMap.getOrDefault(book.getId(), 0L)).reversed())
                        .thenComparing(Comparator.comparingInt(this::getBookRatingCount).reversed())
                        .thenComparing(Comparator.comparingLong(Book::getId).reversed()))
                .limit(getRecommendCount())
                .collect(Collectors.toList());

        return convertToBookVOList(rankedBooks);
    }

    private List<BookVO> calculateCategoryRecommend(Long userId) {
        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId));
        List<BorrowRecord> borrows = borrowRecordMapper.selectList(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId));

        if (favorites.isEmpty() && borrows.isEmpty()) {
            return getHotRecommendBooks();
        }

        Map<Long, Double> categoryScores = new HashMap<>();
        Set<Long> interactedBookIds = new HashSet<>();

        for (BorrowRecord borrow : borrows) {
            accumulateCategoryScore(categoryScores, interactedBookIds, borrow.getBookId(), 1.0);
        }
        for (Favorite favorite : favorites) {
            accumulateCategoryScore(categoryScores, interactedBookIds, favorite.getBookId(), 2.0);
        }

        List<Long> topCategories = categoryScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topCategories.isEmpty()) {
            return filterHotRecommend(interactedBookIds);
        }

        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, Constants.BOOK_STATUS_ON)
                .in(Book::getCategoryId, topCategories);
        if (!interactedBookIds.isEmpty()) {
            wrapper.notIn(Book::getId, interactedBookIds);
        }

        List<Book> candidateBooks = bookMapper.selectList(wrapper);
        if (candidateBooks.isEmpty()) {
            return filterHotRecommend(interactedBookIds);
        }

        List<Long> recommendedBookIds = candidateBooks.stream()
                .sorted(Comparator
                        .comparingDouble((Book book) -> categoryScores.getOrDefault(book.getCategoryId(), 0.0)).reversed()
                        .thenComparing(Comparator.comparingDouble(this::getBookAvgRating).reversed())
                        .thenComparing(Comparator.comparingInt(this::getBookRatingCount).reversed())
                        .thenComparing(Comparator.comparingLong(Book::getId).reversed()))
                .map(Book::getId)
                .collect(Collectors.toList());

        return finalizeRecommendList(recommendedBookIds, interactedBookIds);
    }

    private List<BookVO> calculateLatestRecommend() {
        List<Book> latestBooks = bookMapper.selectList(new LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, Constants.BOOK_STATUS_ON))
                .stream()
                .sorted(Comparator
                        .comparing(Book::getPublishDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Book::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Book::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(getRecommendCount())
                .collect(Collectors.toList());
        return convertToBookVOList(latestBooks);
    }

    private Map<Long, Map<Long, Double>> buildRatingMatrix(List<Map<String, Object>> allRatings) {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();

        for (Map<String, Object> row : allRatings) {
            Long userId = ((Number) row.get("user_id")).longValue();
            Long bookId = ((Number) row.get("book_id")).longValue();
            Double score = ((Number) row.get("score")).doubleValue();
            matrix.computeIfAbsent(userId, key -> new HashMap<>()).put(bookId, score);
        }

        double borrowScore = getImplicitBorrowScore();
        double favoriteScore = getImplicitFavoriteScore();

        for (BorrowRecord borrow : borrowRecordMapper.selectList(new LambdaQueryWrapper<BorrowRecord>())) {
            matrix.computeIfAbsent(borrow.getUserId(), key -> new HashMap<>())
                    .putIfAbsent(borrow.getBookId(), borrowScore);
        }
        for (Favorite favorite : favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>())) {
            matrix.computeIfAbsent(favorite.getUserId(), key -> new HashMap<>())
                    .putIfAbsent(favorite.getBookId(), favoriteScore);
        }

        return matrix;
    }

    private double pearsonSimilarity(Map<Long, Double> ratings1, Map<Long, Double> ratings2) {
        Set<Long> commonBooks = new HashSet<>(ratings1.keySet());
        commonBooks.retainAll(ratings2.keySet());

        if (commonBooks.size() < 2) {
            return 0.0;
        }

        double avg1 = ratings1.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avg2 = ratings2.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double numerator = 0.0;
        double sumSq1 = 0.0;
        double sumSq2 = 0.0;

        for (Long bookId : commonBooks) {
            double diff1 = ratings1.get(bookId) - avg1;
            double diff2 = ratings2.get(bookId) - avg2;
            numerator += diff1 * diff2;
            sumSq1 += diff1 * diff1;
            sumSq2 += diff2 * diff2;
        }

        double denominator = Math.sqrt(sumSq1 * sumSq2);
        return denominator == 0.0 ? 0.0 : numerator / denominator;
    }

    private void accumulateCategoryScore(Map<Long, Double> categoryScores, Set<Long> interactedBookIds,
                                         Long bookId, double weight) {
        interactedBookIds.add(bookId);
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getStatus() != Constants.BOOK_STATUS_ON || book.getCategoryId() == null) {
            return;
        }
        categoryScores.merge(book.getCategoryId(), weight, Double::sum);
    }

    private List<BookVO> finalizeRecommendList(List<Long> primaryBookIds, Set<Long> excludedBookIds) {
        LinkedHashSet<Long> mergedBookIds = new LinkedHashSet<>();
        for (Long bookId : primaryBookIds) {
            if (!excludedBookIds.contains(bookId)) {
                mergedBookIds.add(bookId);
            }
            if (mergedBookIds.size() >= getRecommendCount()) {
                break;
            }
        }

        if (mergedBookIds.size() < getRecommendCount()) {
            for (BookVO hotBook : getHotRecommendBooks()) {
                Long bookId = hotBook.getId();
                if (!excludedBookIds.contains(bookId)) {
                    mergedBookIds.add(bookId);
                }
                if (mergedBookIds.size() >= getRecommendCount()) {
                    break;
                }
            }
        }

        return convertToBookVOListByIds(new ArrayList<>(mergedBookIds));
    }

    private List<BookVO> filterHotRecommend(Set<Long> excludedBookIds) {
        return getHotRecommendBooks().stream()
                .filter(book -> !excludedBookIds.contains(book.getId()))
                .limit(getRecommendCount())
                .collect(Collectors.toList());
    }

    private List<BookVO> getHotRecommendBooks() {
        List<BookVO> cached = hotRecommendCache;
        if (cached == null) {
            synchronized (this) {
                if (hotRecommendCache == null) {
                    hotRecommendCache = calculateHotRecommend();
                }
                cached = hotRecommendCache;
            }
        }
        return cached;
    }

    private List<BookVO> convertToBookVOListByIds(List<Long> bookIds) {
        Map<Long, String> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return bookIds.stream()
                .map(bookMapper::selectById)
                .filter(book -> book != null && book.getStatus() == Constants.BOOK_STATUS_ON)
                .map(book -> {
                    BookVO vo = new BookVO();
                    BeanUtils.copyProperties(book, vo);
                    vo.setCategoryName(categoryMap.getOrDefault(book.getCategoryId(), "未分类"));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private List<BookVO> convertToBookVOList(List<Book> books) {
        Map<Long, String> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return books.stream()
                .filter(Objects::nonNull)
                .map(book -> {
                    BookVO vo = new BookVO();
                    BeanUtils.copyProperties(book, vo);
                    vo.setCategoryName(categoryMap.getOrDefault(book.getCategoryId(), "未分类"));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private double getBookAvgRating(Book book) {
        return book.getAvgRating() != null ? book.getAvgRating().doubleValue() : 0.0;
    }

    private int getBookRatingCount(Book book) {
        return book.getRatingCount() != null ? book.getRatingCount() : 0;
    }

    private int getSimilarUserCount() {
        return Integer.parseInt(algorithmConfigService.getConfigValue("similar_user_count", "20"));
    }

    private int getRecommendCount() {
        return Integer.parseInt(algorithmConfigService.getConfigValue("recommend_count", "10"));
    }

    private double getSimilarityThreshold() {
        return Double.parseDouble(algorithmConfigService.getConfigValue("similarity_threshold", "0.1"));
    }

    private int getColdStartMinRatings() {
        return Integer.parseInt(algorithmConfigService.getConfigValue("cold_start_min_ratings", "5"));
    }

    private int getImplicitBorrowScore() {
        return Integer.parseInt(algorithmConfigService.getConfigValue("implicit_borrow_score", "3"));
    }

    private int getImplicitFavoriteScore() {
        return Integer.parseInt(algorithmConfigService.getConfigValue("implicit_favorite_score", "4"));
    }
}
