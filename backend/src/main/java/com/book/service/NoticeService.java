package com.book.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.book.common.Result;
import com.book.dto.NoticeDTO;
import com.book.entity.Notice;

public interface NoticeService extends IService<Notice> {

    Result<IPage<Notice>> getNoticeList(int page, int size);

    Result<IPage<Notice>> getAdminNoticeList(int page, int size, String keyword, Integer status);

    Result<Notice> getNoticeDetail(Long id);

    Result<?> addNotice(NoticeDTO dto, Long adminId);

    Result<?> updateNotice(NoticeDTO dto);

    Result<?> deleteNotice(Long id);
}
