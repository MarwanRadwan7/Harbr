package com.harbr.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String errorCode;
    private String message;

    private ApiResponse(boolean success, T data, String errorCode, String message) {
        this.success = success;
        this.data = data;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null);
    }

    public static ApiResponse<Void> error(String errorCode, String message) {
        return new ApiResponse<>(false, null, errorCode, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}