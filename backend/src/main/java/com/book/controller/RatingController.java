package com.book.controller;

import com.book.common.Result;
import com.book.dto.RatingDTO;
import com.book.service.RatingService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/rating")
    public Result<?> rateBook(@Validated @RequestBody RatingDTO dto) {
        Long userId = getCurrentUserId();
        return ratingService.rateBook(userId, dto);
    }

    @GetMapping("/rating/book/{bookId}")
    public Result<?> getBookRating(@PathVariable Long bookId) {
        return ratingService.getBookRating(bookId);
    }

    @GetMapping("/rating/my/{bookId}")
    public Result<Integer> getMyRating(@PathVariable Long bookId) {
        Long userId = getCurrentUserId();
        return ratingService.getMyRating(userId, bookId);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
