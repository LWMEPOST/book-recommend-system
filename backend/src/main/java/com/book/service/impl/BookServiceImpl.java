package com.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.book.common.Constants;
import com.book.common.Result;
import com.book.dto.BookDTO;
import com.book.entity.Book;
import com.book.entity.Category;
import com.book.mapper.BookMapper;
import com.book.mapper.CategoryMapper;
import com.book.service.BookService;
import com.book.service.RecommendService;
import com.book.vo.BookVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    private final CategoryMapper categoryMapper;
    private final RecommendService recommendService;

    public BookServiceImpl(CategoryMapper categoryMapper, RecommendService recommendService) {
        this.categoryMapper = categoryMapper;
        this.recommendService = recommendService;
    }

    @Override
    public Result<IPage<BookVO>> getBookList(int page, int size, String keyword, Long categoryId) {
        Page<Book> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, Constants.BOOK_STATUS_ON);
        return Result.success(buildBookVOPage(pageParam, wrapper, keyword, categoryId, null));
    }

    @Override
    public Result<IPage<BookVO>> getAdminBookList(int page, int size, String keyword, Long categoryId, Integer status) {
        Page<Book> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        return Result.success(buildBookVOPage(pageParam, wrapper, keyword, categoryId, status));
    }

    @Override
    public Result<BookVO> getBookDetail(Long id) {
        Book book = getById(id);
        if (book == null) {
            return Result.error("图书不存在");
        }

        BookVO vo = new BookVO();
        BeanUtils.copyProperties(book, vo);

        if (book.getCategoryId() != null) {
            Category category = categoryMapper.selectById(book.getCategoryId());
            vo.setCategoryName(category != null ? category.getName() : "未分类");
        }

        return Result.success(vo);
    }

    @Override
    public Result<?> addBook(BookDTO dto) {
        Book book = new Book();
        BeanUtils.copyProperties(dto, book);
        book.setCover(normalizeCover(book.getCover()));
        if (book.getTotalCount() == null) {
            book.setTotalCount(0);
        }
        if (book.getAvailableCount() == null) {
            book.setAvailableCount(book.getTotalCount());
        }
        if (book.getStatus() == null) {
            book.setStatus(Constants.BOOK_STATUS_ON);
        }
        save(book);
        recommendService.evictAllRecommendCache();
        return Result.success("添加成功", null);
    }

    @Override
    public Result<?> updateBook(Long id, BookDTO dto) {
        Book book = getById(id);
        if (book == null) {
            return Result.error("图书不存在");
        }

        int borrowedCount = Math.max(0, safeInt(book.getTotalCount()) - safeInt(book.getAvailableCount()));
        applyBookDto(book, dto, false);
        book.setAvailableCount(Math.max(0, safeInt(book.getTotalCount()) - borrowedCount));
        updateById(book);
        recommendService.evictAllRecommendCache();
        return Result.success("更新成功", null);
    }

    @Override
    public Result<Map<String, Object>> batchImportBooks(List<BookDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Result.error("请先提供要导入的图书数据");
        }

        Map<Long, String> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        int createdCount = 0;
        int updatedCount = 0;
        List<String> failures = new ArrayList<>();

        for (int index = 0; index < dtoList.size(); index++) {
            BookDTO dto = dtoList.get(index);
            String validationError = validateImportDto(dto, categoryMap);
            if (validationError != null) {
                failures.add("第 " + (index + 1) + " 行：" + validationError);
                continue;
            }

            Book existingBook = findExistingBook(dto);
            if (existingBook == null) {
                Book newBook = new Book();
                applyBookDto(newBook, dto, true);
                newBook.setAvailableCount(safeInt(newBook.getTotalCount()));
                save(newBook);
                createdCount++;
                continue;
            }

            int borrowedCount = Math.max(0, safeInt(existingBook.getTotalCount()) - safeInt(existingBook.getAvailableCount()));
            applyBookDto(existingBook, dto, false);
            existingBook.setAvailableCount(Math.max(0, safeInt(existingBook.getTotalCount()) - borrowedCount));
            updateById(existingBook);
            updatedCount++;
        }

        if (createdCount > 0 || updatedCount > 0) {
            recommendService.evictAllRecommendCache();
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCount", dtoList.size());
        summary.put("createdCount", createdCount);
        summary.put("updatedCount", updatedCount);
        summary.put("failedCount", failures.size());
        summary.put("failures", failures);

        String message = failures.isEmpty()
                ? "批量导入完成"
                : "批量导入完成，部分数据需要处理";
        return Result.success(message, summary);
    }

    @Override
    public Result<?> deleteBook(Long id) {
        removeById(id);
        recommendService.evictAllRecommendCache();
        return Result.success("删除成功", null);
    }

    private IPage<BookVO> buildBookVOPage(Page<Book> pageParam, LambdaQueryWrapper<Book> wrapper,
                                          String keyword, Long categoryId, Integer status) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        if (StringUtils.hasText(normalizedKeyword)) {
            wrapper.and(condition -> condition.like(Book::getTitle, normalizedKeyword)
                    .or().like(Book::getAuthor, normalizedKeyword)
                    .or().like(Book::getIsbn, normalizedKeyword)
                    .or().like(Book::getPublisher, normalizedKeyword)
                    .or().like(Book::getDescription, normalizedKeyword));
        }
        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Book::getStatus, status);
        }
        wrapper.orderByDesc(Book::getCreateTime);

        IPage<Book> bookPage = page(pageParam, wrapper);

        Map<Long, String> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return bookPage.convert(book -> {
            BookVO vo = new BookVO();
            BeanUtils.copyProperties(book, vo);
            vo.setCategoryName(categoryMap.getOrDefault(book.getCategoryId(), "未分类"));
            return vo;
        });
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal safePrice(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeCover(String cover) {
        return StringUtils.hasText(cover) ? cover.trim() : null;
    }

    private void applyBookDto(Book book, BookDTO dto, boolean isNew) {
        if (StringUtils.hasText(dto.getTitle()) || isNew) {
            book.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle().trim() : null);
        }
        if (StringUtils.hasText(dto.getAuthor()) || isNew) {
            book.setAuthor(StringUtils.hasText(dto.getAuthor()) ? dto.getAuthor().trim() : null);
        }
        if (dto.getCategoryId() != null || isNew) {
            book.setCategoryId(dto.getCategoryId());
        }
        if (dto.getPublishDate() != null || isNew) {
            book.setPublishDate(dto.getPublishDate());
        }
        if (dto.getTotalCount() != null || isNew) {
            book.setTotalCount(Math.max(0, safeInt(dto.getTotalCount())));
        }
        if (dto.getPrice() != null || isNew) {
            book.setPrice(safePrice(dto.getPrice()));
        }
        if (dto.getStatus() != null || isNew) {
            book.setStatus(dto.getStatus() != null ? dto.getStatus() : Constants.BOOK_STATUS_ON);
        }

        if (StringUtils.hasText(dto.getIsbn()) || isNew) {
            book.setIsbn(normalizeText(dto.getIsbn()));
        }
        if (StringUtils.hasText(dto.getPublisher()) || isNew) {
            book.setPublisher(normalizeText(dto.getPublisher()));
        }
        if (StringUtils.hasText(dto.getCover()) || isNew) {
            book.setCover(normalizeCover(dto.getCover()));
        }
        if (StringUtils.hasText(dto.getDescription()) || isNew) {
            book.setDescription(normalizeText(dto.getDescription()));
        }

        if (isNew) {
            if (book.getTotalCount() == null) {
                book.setTotalCount(0);
            }
            if (book.getPrice() == null) {
                book.setPrice(BigDecimal.ZERO);
            }
            if (book.getStatus() == null) {
                book.setStatus(Constants.BOOK_STATUS_ON);
            }
        }
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String validateImportDto(BookDTO dto, Map<Long, String> categoryMap) {
        if (dto == null) {
            return "图书数据为空";
        }
        if (!StringUtils.hasText(dto.getTitle())) {
            return "书名不能为空";
        }
        if (!StringUtils.hasText(dto.getAuthor())) {
            return "作者不能为空";
        }
        if (dto.getCategoryId() == null) {
            return "分类不能为空";
        }
        if (!categoryMap.containsKey(dto.getCategoryId())) {
            return "分类不存在";
        }
        if (dto.getTotalCount() != null && dto.getTotalCount() < 0) {
            return "馆藏数量不能小于 0";
        }
        if (dto.getPrice() != null && dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            return "价格不能小于 0";
        }
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            return "状态只能为 0 或 1";
        }
        return null;
    }

    private Book findExistingBook(BookDTO dto) {
        String isbn = normalizeText(dto.getIsbn());
        if (StringUtils.hasText(isbn)) {
            Book byIsbn = lambdaQuery()
                    .eq(Book::getIsbn, isbn)
                    .last("LIMIT 1")
                    .one();
            if (byIsbn != null) {
                return byIsbn;
            }
        }

        return lambdaQuery()
                .eq(Book::getTitle, dto.getTitle().trim())
                .eq(Book::getAuthor, dto.getAuthor().trim())
                .last("LIMIT 1")
                .one();
    }
}
