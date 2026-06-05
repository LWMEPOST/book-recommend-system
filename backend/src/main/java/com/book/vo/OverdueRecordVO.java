package com.book.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OverdueRecordVO {

    private Long id;
    private Long borrowId;
    private Long userId;
    private String username;
    private Long bookId;
    private String bookTitle;
    private Integer overdueDays;
    private BigDecimal fine;
    private Integer status;
    private LocalDateTime createTime;

    public OverdueRecordVO() {
    }

    public OverdueRecordVO(Long id, Long borrowId, Long userId, String username, Long bookId, String bookTitle, Integer overdueDays, BigDecimal fine, Integer status, LocalDateTime createTime) {
        this.id = id;
        this.borrowId = borrowId;
        this.userId = userId;
        this.username = username;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.overdueDays = overdueDays;
        this.fine = fine;
        this.status = status;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBorrowId() {
        return borrowId;
    }

    public void setBorrowId(Long borrowId) {
        this.borrowId = borrowId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public Integer getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(Integer overdueDays) {
        this.overdueDays = overdueDays;
    }

    public BigDecimal getFine() {
        return fine;
    }

    public void setFine(BigDecimal fine) {
        this.fine = fine;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
