package com.keepsake.backend.common;

import java.util.List;

/** Simple error body returned for every failed request. */
public record ErrorResponse(String message, int status, List<String> fieldErrors) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(message, status, null);
    }
}
