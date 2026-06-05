package com.book.controller;

import com.book.common.Result;
import com.book.service.RecommendService;
import com.book.vo.BookVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping("/recommend/personal")
    public Result<List<BookVO>> getPersonalRecommend() {
        Long userId = getCurrentUserId();
        return recommendService.getPersonalRecommend(userId);
    }

    @GetMapping("/recommend/hot")
    public Result<List<BookVO>> getHotRecommend() {
        return recommendService.getHotRecommend();
    }

    @GetMapping("/recommend/latest")
    public Result<List<BookVO>> getLatestRecommend() {
        return recommendService.getLatestRecommend();
    }

    @GetMapping("/recommend/category")
    public Result<List<BookVO>> getCategoryRecommend() {
        Long userId = getCurrentUserId();
        return recommendService.getCategoryRecommend(userId);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return (Long) authentication.getPrincipal();
    }
}
