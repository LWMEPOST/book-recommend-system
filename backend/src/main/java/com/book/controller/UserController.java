package com.book.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.book.common.Constants;
import com.book.common.Result;
import com.book.dto.*;
import com.book.entity.User;
import com.book.service.UserService;
import com.book.vo.LoginVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/user/register")
    public Result<?> register(@Validated @RequestBody RegisterDTO dto) {
        return userService.register(dto);
    }

    @PostMapping("/user/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        return userService.login(dto);
    }

    @GetMapping("/user/info")
    public Result<User> getUserInfo() {
        Long userId = getCurrentUserId();
        return userService.getUserInfo(userId);
    }

    @PutMapping("/user/info")
    public Result<?> updateUserInfo(@RequestBody UserUpdateDTO dto) {
        Long userId = getCurrentUserId();
        return userService.updateUserInfo(userId, dto);
    }

    @PutMapping("/user/password")
    public Result<?> updatePassword(@Validated @RequestBody PasswordDTO dto) {
        Long userId = getCurrentUserId();
        return userService.updatePassword(userId, dto);
    }

    @GetMapping("/admin/user/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<User>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return userService.getUserList(page, size, keyword);
    }

    @PutMapping("/admin/user/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> updateUserStatus(@RequestParam Long userId, @RequestParam Integer status) {
        return userService.updateUserStatus(userId, status);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
