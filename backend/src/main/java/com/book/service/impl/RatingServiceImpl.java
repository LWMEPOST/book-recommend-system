package com.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.book.common.Result;
import com.book.dto.RatingDTO;
import com.book.entity.Book;
import com.book.entity.Rating;
import com.book.mapper.BookMapper;
import com.book.mapper.RatingMapper;
import com.book.service.RatingService;
import com.book.service.RecommendService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RatingServiceImpl extends ServiceImpl<RatingMapper, Rating> implements RatingService {

    private final BookMapper bookMapper;
    private final RecommendService recommendService;

    public RatingServiceImpl(BookMapper bookMapper, @Lazy RecommendService recommendService) {
        this.bookMapper = bookMapper;
        this.recommendService = recommendService;
    }

    @Override
    @Transactional
    public Result<?> rateBook(Long userId, RatingDTO dto) {
        LambdaQueryWrapper<Rating> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Rating::getUserId, userId)
                .eq(Rating::getBookId, dto.getBookId());
        Rating existing = getOne(wrapper);

        if (existing != null) {
            existing.setScore(dto.getScore());
            updateById(existing);
        } else {
            Rating rating = new Rating();
            rating.setUserId(userId);
            rating.setBookId(dto.getBookId());
            rating.setScore(dto.getScore());
            save(rating);
        }

        updateBookRating(dto.getBookId());
        recommendService.evictAllRecommendCache();

        return Result.success(existing != null ? "评分已更新" : "评分成功", null);
    }

    @Override
    public Result<?> getBookRating(Long bookId) {
        LambdaQueryWrapper<Rating> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Rating::getBookId, bookId);
        List<Rating> ratings = list(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("ratingCount", ratings.size());

        if (ratings.isEmpty()) {
            result.put("avgRating", 0.0);
        } else {
            double avg = ratings.stream().mapToInt(Rating::getScore).average().orElse(0.0);
            result.put("avgRating", BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue());
        }

        return Result.success(result);
    }

    @Override
    public Result<Integer> getMyRating(Long userId, Long bookId) {
        LambdaQueryWrapper<Rating> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Rating::getUserId, userId)
                .eq(Rating::getBookId, bookId);
        Rating rating = getOne(wrapper);
        return Result.success(rating != null ? rating.getScore() : 0);
    }

    @Override
    public List<Map<String, Object>> getAllRatings() {
        return baseMapper.selectAllRatings();
    }

    private void updateBookRating(Long bookId) {
        LambdaQueryWrapper<Rating> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Rating::getBookId, bookId);
        List<Rating> ratings = list(wrapper);

        Book book = bookMapper.selectById(bookId);
        if (book != null) {
            book.setRatingCount(ratings.size());
            if (!ratings.isEmpty()) {
                double avg = ratings.stream().mapToInt(Rating::getScore).average().orElse(0.0);
                book.setAvgRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
            } else {
                book.setAvgRating(BigDecimal.ZERO);
            }
            bookMapper.updateById(book);
        }
    }
}
