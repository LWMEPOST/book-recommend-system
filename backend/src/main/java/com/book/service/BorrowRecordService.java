package com.book.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.book.common.Result;
import com.book.entity.BorrowRecord;
import com.book.vo.BorrowRecordVO;

public interface BorrowRecordService extends IService<BorrowRecord> {

    Result<?> borrowBook(Long userId, Long bookId);

    Result<?> returnBook(Long userId, Long recordId);

    Result<IPage<BorrowRecordVO>> getMyBorrowList(Long userId, int page, int size);

    Result<IPage<BorrowRecordVO>> getAdminBorrowList(int page, int size, Integer status, String keyword);
}
