package com.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.book.common.Constants;
import com.book.common.Result;
import com.book.dto.NoticeDTO;
import com.book.entity.Notice;
import com.book.mapper.NoticeMapper;
import com.book.service.NoticeService;
import org.springframework.stereotype.Service;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Override
    public Result<IPage<Notice>> getNoticeList(int page, int size) {
        Page<Notice> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getStatus, Constants.NOTICE_STATUS_PUBLISHED)
                .orderByDesc(Notice::getCreateTime);
        return Result.success(page(pageParam, wrapper));
    }

    @Override
    public Result<IPage<Notice>> getAdminNoticeList(int page, int size, String keyword, Integer status) {
        Page<Notice> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Notice::getTitle, keyword)
                    .or().like(Notice::getContent, keyword));
        }
        if (status != null) {
            wrapper.eq(Notice::getStatus, status);
        }
        wrapper.orderByDesc(Notice::getCreateTime);
        return Result.success(page(pageParam, wrapper));
    }

    @Override
    public Result<Notice> getNoticeDetail(Long id) {
        Notice notice = getById(id);
        if (notice == null) {
            return Result.error("公告不存在");
        }
        if (notice.getStatus() != Constants.NOTICE_STATUS_PUBLISHED) {
            return Result.error("公告不存在");
        }
        return Result.success(notice);
    }

    @Override
    public Result<?> addNotice(NoticeDTO dto, Long adminId) {
        Notice notice = new Notice();
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setAdminId(adminId);
        notice.setStatus(dto.getStatus() != null ? dto.getStatus() : Constants.NOTICE_STATUS_PUBLISHED);
        save(notice);
        return Result.success("发布成功", null);
    }

    @Override
    public Result<?> updateNotice(NoticeDTO dto) {
        Notice notice = getById(dto.getId());
        if (notice == null) {
            return Result.error("公告不存在");
        }
        if (dto.getTitle() != null) notice.setTitle(dto.getTitle());
        if (dto.getContent() != null) notice.setContent(dto.getContent());
        if (dto.getStatus() != null) notice.setStatus(dto.getStatus());
        updateById(notice);
        return Result.success("更新成功", null);
    }

    @Override
    public Result<?> deleteNotice(Long id) {
        removeById(id);
        return Result.success("删除成功", null);
    }
}
