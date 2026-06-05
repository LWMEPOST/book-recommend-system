package com.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.book.common.Constants;
import com.book.common.Result;
import com.book.entity.Book;
import com.book.entity.BorrowRecord;
import com.book.entity.OverdueRecord;
import com.book.entity.User;
import com.book.mapper.BookMapper;
import com.book.mapper.BorrowRecordMapper;
import com.book.mapper.OverdueRecordMapper;
import com.book.mapper.UserMapper;
import com.book.service.OverdueRecordService;
import com.book.vo.OverdueRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OverdueRecordServiceImpl extends ServiceImpl<OverdueRecordMapper, OverdueRecord> implements OverdueRecordService {

    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;

    public OverdueRecordServiceImpl(UserMapper userMapper, BookMapper bookMapper, BorrowRecordMapper borrowRecordMapper) {
        this.userMapper = userMapper;
        this.bookMapper = bookMapper;
        this.borrowRecordMapper = borrowRecordMapper;
    }

    @Override
    public synchronized void createOverdueRecord(BorrowRecord borrowRecord, int overdueDays) {
        OverdueRecord overdueRecord = getOne(new LambdaQueryWrapper<OverdueRecord>()
                .eq(OverdueRecord::getBorrowId, borrowRecord.getId()), false);
        BigDecimal fine = BigDecimal.valueOf(overdueDays * Constants.DAILY_FINE);

        if (overdueRecord == null) {
            overdueRecord = new OverdueRecord();
            overdueRecord.setBorrowId(borrowRecord.getId());
            overdueRecord.setUserId(borrowRecord.getUserId());
            overdueRecord.setBookId(borrowRecord.getBookId());
            overdueRecord.setOverdueDays(overdueDays);
            overdueRecord.setFine(fine);
            overdueRecord.setStatus(Constants.OVERDUE_STATUS_PENDING);
            save(overdueRecord);
            return;
        }

        overdueRecord.setOverdueDays(overdueDays);
        overdueRecord.setFine(fine);
        updateById(overdueRecord);
    }

    @Override
    public void syncActiveOverdueRecords(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getStatus, Constants.BORROW_STATUS_BORROWING)
                .isNull(BorrowRecord::getReturnDate)
                .lt(BorrowRecord::getDueDate, now);
        if (userId != null) {
            wrapper.eq(BorrowRecord::getUserId, userId);
        }

        List<BorrowRecord> overdueBorrows = borrowRecordMapper.selectList(wrapper);
        for (BorrowRecord borrowRecord : overdueBorrows) {
            int overdueDays = (int) Math.max(ChronoUnit.DAYS.between(borrowRecord.getDueDate(), now), 0);
            createOverdueRecord(borrowRecord, overdueDays);
        }
    }

    @Override
    public Result<IPage<OverdueRecordVO>> getMyOverdueList(Long userId, int page, int size) {
        syncActiveOverdueRecords(userId);
        Page<OverdueRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<OverdueRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OverdueRecord::getUserId, userId)
                .orderByDesc(OverdueRecord::getCreateTime);
        IPage<OverdueRecord> recordPage = page(pageParam, wrapper);
        IPage<OverdueRecordVO> voPage = recordPage.convert(this::convertToVO);
        return Result.success(voPage);
    }

    @Override
    public Result<IPage<OverdueRecordVO>> getAdminOverdueList(int page, int size, Integer status, String keyword) {
        syncActiveOverdueRecords(null);
        Page<OverdueRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<OverdueRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(OverdueRecord::getStatus, status);
        }
        if (!applyKeywordFilter(wrapper, keyword)) {
            return Result.success(new Page<>(page, size));
        }
        wrapper.orderByDesc(OverdueRecord::getCreateTime);
        IPage<OverdueRecord> recordPage = page(pageParam, wrapper);
        IPage<OverdueRecordVO> voPage = recordPage.convert(this::convertToVO);
        return Result.success(voPage);
    }

    @Override
    public Result<?> handleOverdue(Long id) {
        OverdueRecord record = getById(id);
        if (record == null) {
            return Result.error("逾期记录不存在");
        }
        record.setStatus(Constants.OVERDUE_STATUS_HANDLED);
        updateById(record);
        return Result.success("处理成功", null);
    }

    private OverdueRecordVO convertToVO(OverdueRecord record) {
        OverdueRecordVO vo = new OverdueRecordVO();
        BeanUtils.copyProperties(record, vo);

        User user = userMapper.selectById(record.getUserId());
        if (user != null) {
            vo.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
        }

        Book book = bookMapper.selectById(record.getBookId());
        if (book != null) {
            vo.setBookTitle(book.getTitle());
        }

        return vo;
    }

    private boolean applyKeywordFilter(LambdaQueryWrapper<OverdueRecord> wrapper, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }

        List<Long> matchedUserIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .like(User::getUsername, keyword)
                        .or()
                        .like(User::getNickname, keyword))
                .stream()
                .map(User::getId)
                .collect(Collectors.toList());

        List<Long> matchedBookIds = bookMapper.selectList(new LambdaQueryWrapper<Book>()
                        .like(Book::getTitle, keyword)
                        .or()
                        .like(Book::getAuthor, keyword)
                        .or()
                        .like(Book::getIsbn, keyword))
                .stream()
                .map(Book::getId)
                .collect(Collectors.toList());

        if (matchedUserIds.isEmpty() && matchedBookIds.isEmpty()) {
            return false;
        }

        wrapper.and(w -> {
            if (!matchedUserIds.isEmpty()) {
                w.in(OverdueRecord::getUserId, matchedUserIds);
            }
            if (!matchedBookIds.isEmpty()) {
                if (!matchedUserIds.isEmpty()) {
                    w.or();
                }
                w.in(OverdueRecord::getBookId, matchedBookIds);
            }
        });
        return true;
    }
}
