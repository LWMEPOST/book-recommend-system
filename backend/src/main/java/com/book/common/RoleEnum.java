package com.book.common;

public enum RoleEnum {

    USER(0, "普通用户"),
    ADMIN(1, "管理员");

    private final int code;
    private final String desc;

    RoleEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
