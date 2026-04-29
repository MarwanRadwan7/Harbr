package com.harbr.common.web;

import java.util.Collections;
import java.util.List;

public record ApiError(String code, String message, List<FieldError> fieldErrors) {

    public ApiError(String code, String message) {
        this(code, message, Collections.emptyList());
    }

    public record FieldError(String field, String message) {
    }
}