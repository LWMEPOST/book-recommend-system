package com.book.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.book.common.Result;
import com.book.dto.*;
import com.book.entity.User;
import com.book.vo.LoginVO;

public interface UserService extends IService<User> {

    Result<?> register(RegisterDTO dto);

    Result<LoginVO> login(LoginDTO dto);

    Result<User> getUserInfo(Long userId);

    Result<?> updateUserInfo(Long userId, UserUpdateDTO dto);

    Result<?> updatePassword(Long userId, PasswordDTO dto);

    Result<IPage<User>> getUserList(int page, int size, String keyword);

    Result<?> updateUserStatus(Long userId, Integer status);
}
