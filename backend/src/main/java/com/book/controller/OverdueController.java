package com.book.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.book.common.Result;
import com.book.service.OverdueRecordService;
import com.book.vo.OverdueRecordVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OverdueController {

    private final OverdueRecordService overdueRecordService;

    public OverdueController(OverdueRecordService overdueRecordService) {
        this.overdueRecordService = overdueRecordService;
    }

    @GetMapping("/overdue/my")
    public Result<IPage<OverdueRecordVO>> getMyOverdueList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        return overdueRecordService.getMyOverdueList(userId, page, size);
    }

    @GetMapping("/admin/overdue/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<OverdueRecordVO>> getAdminOverdueList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return overdueRecordService.getAdminOverdueList(page, size, status, keyword);
    }

    @PutMapping("/admin/overdue/handle/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> handleOverdue(@PathVariable Long id) {
        return overdueRecordService.handleOverdue(id);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
