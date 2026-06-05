package com.book.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.book.common.Result;
import com.book.service.BorrowRecordService;
import com.book.vo.BorrowRecordVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BorrowController {

    private final BorrowRecordService borrowRecordService;

    public BorrowController(BorrowRecordService borrowRecordService) {
        this.borrowRecordService = borrowRecordService;
    }

    @PostMapping("/borrow/{bookId}")
    public Result<?> borrowBook(@PathVariable Long bookId) {
        Long userId = getCurrentUserId();
        return borrowRecordService.borrowBook(userId, bookId);
    }

    @PutMapping("/borrow/return/{id}")
    public Result<?> returnBook(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return borrowRecordService.returnBook(userId, id);
    }

    @GetMapping("/borrow/my")
    public Result<IPage<BorrowRecordVO>> getMyBorrowList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        return borrowRecordService.getMyBorrowList(userId, page, size);
    }

    @GetMapping("/admin/borrow/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<BorrowRecordVO>> getAdminBorrowList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return borrowRecordService.getAdminBorrowList(page, size, status, keyword);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
