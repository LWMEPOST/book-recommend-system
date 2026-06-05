package com.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.book.common.Constants;
import com.book.common.Result;
import com.book.entity.Book;
import com.book.entity.BorrowRecord;
import com.book.entity.User;
import com.book.mapper.BookMapper;
import com.book.mapper.BorrowRecordMapper;
import com.book.mapper.UserMapper;
import com.book.service.BorrowRecordService;
import com.book.service.OverdueRecordService;
import com.book.service.RecommendService;
import com.book.vo.BorrowRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BorrowRecordServiceImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord> implements BorrowRecordService {

    private final BookMapper bookMapper;
    private final UserMapper userMapper;
    private final OverdueRecordService overdueRecordService;
    private final RecommendService recommendService;

    public BorrowRecordServiceImpl(BookMapper bookMapper, UserMapper userMapper,
                                   @Lazy OverdueRecordService overdueRecordService,
                                   RecommendService recommendService) {
        this.bookMapper = bookMapper;
        this.userMapper = userMapper;
        this.overdueRecordService = overdueRecordService;
        this.recommendService = recommendService;
    }

    @Override
    @Transactional
    public Result<?> borrowBook(Long userId, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getStatus() == Constants.BOOK_STATUS_OFF) {
            return Result.error("图书不存在或已下架");
        }
        if (book.getAvailableCount() <= 0) {
            return Result.error("图书库存不足");
        }

        Long borrowingCount = lambdaQuery()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getStatus, Constants.BORROW_STATUS_BORROWING)
                .count();
        if (borrowingCount >= 10) {
            return Result.error("借阅数量已达上限(10本)");
        }

        Long alreadyBorrowed = lambdaQuery()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getBookId, bookId)
                .eq(BorrowRecord::getStatus, Constants.BORROW_STATUS_BORROWING)
                .count();
        if (alreadyBorrowed > 0) {
            return Result.error("您已借阅该图书，请先归还");
        }

        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowDate(LocalDateTime.now());
        record.setDueDate(LocalDateTime.now().plusDays(Constants.DEFAULT_BORROW_DAYS));
        record.setStatus(Constants.BORROW_STATUS_BORROWING);
        save(record);

        book.setAvailableCount(book.getAvailableCount() - 1);
        bookMapper.updateById(book);
        recommendService.evictAllRecommendCache();

        return Result.success("借阅成功", null);
    }

    @Override
    @Transactional
    public Result<?> returnBook(Long userId, Long recordId) {
        BorrowRecord record = getById(recordId);
        if (record == null) {
            return Result.error("借阅记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            return Result.error("无权操作此记录");
        }
        if (record.getStatus() == Constants.BORROW_STATUS_RETURNED) {
            return Result.error("图书已归还");
        }

        record.setReturnDate(LocalDateTime.now());
        record.setStatus(Constants.BORROW_STATUS_RETURNED);
        updateById(record);

        Book book = bookMapper.selectById(record.getBookId());
        if (book != null) {
            book.setAvailableCount(book.getAvailableCount() + 1);
            bookMapper.updateById(book);
        }

        if (record.getDueDate().isBefore(LocalDateTime.now())) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), LocalDateTime.now());
            overdueRecordService.createOverdueRecord(record, (int) overdueDays);
        }

        recommendService.evictAllRecommendCache();
        return Result.success("归还成功", null);
    }

    @Override
    public Result<IPage<BorrowRecordVO>> getMyBorrowList(Long userId, int page, int size) {
        Page<BorrowRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getUserId, userId)
                .orderByDesc(BorrowRecord::getCreateTime);
        IPage<BorrowRecord> recordPage = page(pageParam, wrapper);

        IPage<BorrowRecordVO> voPage = recordPage.convert(this::convertToVO);
        return Result.success(voPage);
    }

    @Override
    public Result<IPage<BorrowRecordVO>> getAdminBorrowList(int page, int size, Integer status, String keyword) {
        Page<BorrowRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            LocalDateTime now = LocalDateTime.now();
            if (status == Constants.BORROW_STATUS_BORROWING) {
                wrapper.eq(BorrowRecord::getStatus, Constants.BORROW_STATUS_BORROWING)
                        .isNull(BorrowRecord::getReturnDate)
                        .ge(BorrowRecord::getDueDate, now);
            } else if (status == Constants.BORROW_STATUS_OVERDUE) {
                wrapper.eq(BorrowRecord::getStatus, Constants.BORROW_STATUS_BORROWING)
                        .isNull(BorrowRecord::getReturnDate)
                        .lt(BorrowRecord::getDueDate, now);
            } else {
                wrapper.eq(BorrowRecord::getStatus, status);
            }
        }
        if (!applyKeywordFilter(wrapper, keyword)) {
            return Result.success(new Page<>(page, size));
        }
        wrapper.orderByDesc(BorrowRecord::getCreateTime);
        IPage<BorrowRecord> recordPage = page(pageParam, wrapper);

        IPage<BorrowRecordVO> voPage = recordPage.convert(this::convertToVO);
        return Result.success(voPage);
    }

    private BorrowRecordVO convertToVO(BorrowRecord record) {
        BorrowRecordVO vo = new BorrowRecordVO();
        BeanUtils.copyProperties(record, vo);
        vo.setStatus(resolveDisplayStatus(record));

        User user = userMapper.selectById(record.getUserId());
        if (user != null) {
            vo.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
        }

        Book book = bookMapper.selectById(record.getBookId());
        if (book != null) {
            vo.setBookTitle(book.getTitle());
            vo.setBookAuthor(book.getAuthor());
            vo.setBookCover(book.getCover());
        }

        return vo;
    }

    private Integer resolveDisplayStatus(BorrowRecord record) {
        if (record.getStatus() != null
                && record.getStatus() == Constants.BORROW_STATUS_BORROWING
                && record.getReturnDate() == null
                && record.getDueDate() != null
                && record.getDueDate().isBefore(LocalDateTime.now())) {
            return Constants.BORROW_STATUS_OVERDUE;
        }
        return record.getStatus();
    }

    private boolean applyKeywordFilter(LambdaQueryWrapper<BorrowRecord> wrapper, String keyword) {
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

        wrapper.and(condition -> {
            if (!matchedUserIds.isEmpty()) {
                condition.in(BorrowRecord::getUserId, matchedUserIds);
            }
            if (!matchedBookIds.isEmpty()) {
                if (!matchedUserIds.isEmpty()) {
                    condition.or();
                }
                condition.in(BorrowRecord::getBookId, matchedBookIds);
            }
        });
        return true;
    }
}
