package com.book.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.book.common.Result;
import com.book.dto.BookDTO;
import com.book.entity.Category;
import com.book.service.BookService;
import com.book.service.CategoryService;
import com.book.vo.BookVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;

    public BookController(BookService bookService, CategoryService categoryService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
    }

    @GetMapping("/book/list")
    public Result<IPage<BookVO>> getBookList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        return bookService.getBookList(page, size, keyword, categoryId);
    }

    @GetMapping("/admin/book/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<BookVO>> getAdminBookList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        return bookService.getAdminBookList(page, size, keyword, categoryId, status);
    }

    @GetMapping("/book/{id}")
    public Result<BookVO> getBookDetail(@PathVariable Long id) {
        return bookService.getBookDetail(id);
    }

    @PostMapping("/admin/book")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> addBook(@RequestBody BookDTO dto) {
        return bookService.addBook(dto);
    }

    @PutMapping("/admin/book")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> updateBook(@RequestParam Long id, @RequestBody BookDTO dto) {
        return bookService.updateBook(id, dto);
    }

    @PostMapping("/admin/book/import")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> batchImportBooks(@RequestBody List<BookDTO> dtoList) {
        return bookService.batchImportBooks(dtoList);
    }

    @DeleteMapping("/admin/book/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> deleteBook(@PathVariable Long id) {
        return bookService.deleteBook(id);
    }

    @GetMapping("/category/list")
    public Result<List<Category>> getCategoryList() {
        return categoryService.getCategoryList();
    }
}
