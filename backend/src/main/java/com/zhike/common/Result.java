package com.zhike.common;

/**
 * 统一响应体。code=0 表示成功，其余为业务/认证错误码。
 * 用 record 而不是 Lombok 类，序列化行为由 Jackson 自动适配（Spring Boot 3 原生支持）。
 */
public record Result<T>(int code, String msg, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static Result<Void> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}
