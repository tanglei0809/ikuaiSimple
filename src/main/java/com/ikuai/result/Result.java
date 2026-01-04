package com.ikuai.result;

import com.fasterxml.jackson.annotation.JsonRawValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class Result<T> {
    private int code;
    private String msg;
    private T data;

    // 私有构造，强制使用静态方法构建
    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 核心静态包装方法：将 Result 转为 ResponseEntity
    public static <T> ResponseEntity<Result<T>> out(HttpStatus status, String msg, T data) {
        Result<T> result = new Result<>(status.value(), msg, data);
        return new ResponseEntity<>(result, status);
    }

    // 快捷方法：成功 (200 OK)
    public static <T> ResponseEntity<Result<T>> ok(T data) {
        return out(HttpStatus.OK, "操作成功", data);
    }

    // 快捷方法：失败 (自定义状态码)
    public static <T> ResponseEntity<Result<T>> fail(HttpStatus status, String msg) {
        return out(status, msg, null);
    }
    // 快捷方法：失败 (自定义状态码)
    public static <T> ResponseEntity<Result<T>> fail(String msg) {
        return out(HttpStatus.INTERNAL_SERVER_ERROR, msg, null);
    }


    // Getter 必须有，Jackson 才能转 JSON
    public int getCode() { return code; }
    public String getMsg() { return msg; }
    public T getData() { return data; }

}