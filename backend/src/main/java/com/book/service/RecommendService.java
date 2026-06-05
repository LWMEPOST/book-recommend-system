package com.book.service;

import com.book.common.Result;
import com.book.vo.BookVO;

import java.util.List;
import java.util.Map;

public interface RecommendService {

    Result<List<BookVO>> getPersonalRecommend(Long userId);

    Result<List<BookVO>> getHotRecommend();

    Result<List<BookVO>> getLatestRecommend();

    Result<List<BookVO>> getCategoryRecommend(Long userId);

    Result<Map<String, Object>> refreshRecommendCache();

    Result<Map<String, Object>> getRecommendSummary();

    void evictAllRecommendCache();
}
