package com.library.common;

public record Result<T>(Integer code, String message, T data) {
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data);
    }
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(200, message, data);
    }
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
}
