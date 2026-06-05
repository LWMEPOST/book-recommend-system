package com.book.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.book.common.Result;
import com.book.dto.RatingDTO;
import com.book.entity.Rating;

import java.util.List;
import java.util.Map;

public interface RatingService extends IService<Rating> {

    Result<?> rateBook(Long userId, RatingDTO dto);

    Result<?> getBookRating(Long bookId);

    Result<Integer> getMyRating(Long userId, Long bookId);

    List<Map<String, Object>> getAllRatings();
}
