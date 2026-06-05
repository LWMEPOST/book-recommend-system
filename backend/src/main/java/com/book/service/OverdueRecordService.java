package com.book.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.book.common.Result;
import com.book.entity.BorrowRecord;
import com.book.entity.OverdueRecord;
import com.book.vo.OverdueRecordVO;

public interface OverdueRecordService extends IService<OverdueRecord> {

    void createOverdueRecord(BorrowRecord borrowRecord, int overdueDays);

    void syncActiveOverdueRecords(Long userId);

    Result<IPage<OverdueRecordVO>> getMyOverdueList(Long userId, int page, int size);

    Result<IPage<OverdueRecordVO>> getAdminOverdueList(int page, int size, Integer status, String keyword);

    Result<?> handleOverdue(Long id);
}
