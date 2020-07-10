package com.next.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SYSTEM_ERROR(1, "系统异常"),
    USER_NOT_LOGIN(2, "用户未登录");

    ErrorCode(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    int id;
    String desc;
}
