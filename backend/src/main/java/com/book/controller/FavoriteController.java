package com.book.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.book.common.Result;
import com.book.service.FavoriteService;
import com.book.vo.BookVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/favorite/{bookId}")
    public Result<?> addFavorite(@PathVariable Long bookId) {
        Long userId = getCurrentUserId();
        return favoriteService.addFavorite(userId, bookId);
    }

    @DeleteMapping("/favorite/{bookId}")
    public Result<?> removeFavorite(@PathVariable Long bookId) {
        Long userId = getCurrentUserId();
        return favoriteService.removeFavorite(userId, bookId);
    }

    @GetMapping("/favorite/{bookId}/status")
    public Result<Boolean> getFavoriteStatus(@PathVariable Long bookId) {
        Long userId = getCurrentUserId();
        return Result.success(favoriteService.isFavorite(userId, bookId));
    }

    @GetMapping("/favorite/my")
    public Result<IPage<BookVO>> getMyFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        return favoriteService.getMyFavorites(userId, page, size);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
