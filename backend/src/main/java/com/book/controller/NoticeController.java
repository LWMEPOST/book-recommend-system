package com.book.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.book.common.Result;
import com.book.dto.NoticeDTO;
import com.book.entity.Notice;
import com.book.service.NoticeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/notice/list")
    public Result<IPage<Notice>> getNoticeList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return noticeService.getNoticeList(page, size);
    }

    @GetMapping("/admin/notice/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<Notice>> getAdminNoticeList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return noticeService.getAdminNoticeList(page, size, keyword, status);
    }

    @GetMapping("/notice/{id}")
    public Result<Notice> getNoticeDetail(@PathVariable Long id) {
        return noticeService.getNoticeDetail(id);
    }

    @PostMapping("/admin/notice")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> addNotice(@RequestBody NoticeDTO dto) {
        Long adminId = getCurrentUserId();
        return noticeService.addNotice(dto, adminId);
    }

    @PutMapping("/admin/notice")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> updateNotice(@RequestBody NoticeDTO dto) {
        return noticeService.updateNotice(dto);
    }

    @DeleteMapping("/admin/notice/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> deleteNotice(@PathVariable Long id) {
        return noticeService.deleteNotice(id);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
