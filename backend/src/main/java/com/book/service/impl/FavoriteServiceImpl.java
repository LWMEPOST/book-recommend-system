package com.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.book.common.Result;
import com.book.entity.Book;
import com.book.entity.Category;
import com.book.entity.Favorite;
import com.book.mapper.BookMapper;
import com.book.mapper.CategoryMapper;
import com.book.mapper.FavoriteMapper;
import com.book.service.FavoriteService;
import com.book.service.RecommendService;
import com.book.vo.BookVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final RecommendService recommendService;

    public FavoriteServiceImpl(BookMapper bookMapper, CategoryMapper categoryMapper,
                               RecommendService recommendService) {
        this.bookMapper = bookMapper;
        this.categoryMapper = categoryMapper;
        this.recommendService = recommendService;
    }

    @Override
    public Result<?> addFavorite(Long userId, Long bookId) {
        if (isFavorite(userId, bookId)) {
            return Result.error("已收藏该图书");
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setBookId(bookId);
        save(favorite);
        recommendService.evictAllRecommendCache();
        return Result.success("收藏成功", null);
    }

    @Override
    public Result<?> removeFavorite(Long userId, Long bookId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getBookId, bookId);
        remove(wrapper);
        recommendService.evictAllRecommendCache();
        return Result.success("取消收藏成功", null);
    }

    @Override
    public Result<IPage<BookVO>> getMyFavorites(Long userId, int page, int size) {
        Page<Favorite> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime);
        IPage<Favorite> favoritePage = page(pageParam, wrapper);

        Map<Long, String> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        IPage<BookVO> voPage = favoritePage.convert(fav -> {
            Book book = bookMapper.selectById(fav.getBookId());
            BookVO vo = new BookVO();
            if (book != null) {
                BeanUtils.copyProperties(book, vo);
                vo.setCategoryName(categoryMap.getOrDefault(book.getCategoryId(), "未分类"));
            }
            return vo;
        });

        return Result.success(voPage);
    }

    @Override
    public boolean isFavorite(Long userId, Long bookId) {
        return lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getBookId, bookId)
                .count() > 0;
    }
}
