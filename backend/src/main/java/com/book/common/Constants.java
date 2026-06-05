package com.book.common;

public class Constants {

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_HEADER = "Authorization";
    public static final String USER_ID_KEY = "userId";
    public static final String USERNAME_KEY = "username";
    public static final String ROLE_KEY = "role";

    public static final int ROLE_USER = 0;
    public static final int ROLE_ADMIN = 1;

    public static final int BORROW_STATUS_BORROWING = 0;
    public static final int BORROW_STATUS_RETURNED = 1;
    public static final int BORROW_STATUS_OVERDUE = 2;

    public static final int OVERDUE_STATUS_PENDING = 0;
    public static final int OVERDUE_STATUS_HANDLED = 1;

    public static final int BOOK_STATUS_OFF = 0;
    public static final int BOOK_STATUS_ON = 1;

    public static final int USER_STATUS_DISABLED = 0;
    public static final int USER_STATUS_NORMAL = 1;

    public static final int NOTICE_STATUS_DRAFT = 0;
    public static final int NOTICE_STATUS_PUBLISHED = 1;

    public static final int DEFAULT_BORROW_DAYS = 30;
    public static final double DAILY_FINE = 0.5;
}
