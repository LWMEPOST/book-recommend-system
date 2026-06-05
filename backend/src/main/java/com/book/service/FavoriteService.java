package com.book.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.book.common.Result;
import com.book.entity.Favorite;
import com.book.vo.BookVO;

public interface FavoriteService extends IService<Favorite> {

    Result<?> addFavorite(Long userId, Long bookId);

    Result<?> removeFavorite(Long userId, Long bookId);

    Result<IPage<BookVO>> getMyFavorites(Long userId, int page, int size);

    boolean isFavorite(Long userId, Long bookId);
}
