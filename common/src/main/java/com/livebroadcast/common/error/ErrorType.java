package com.livebroadcast.common.error;

/**
 * 도메인 예외의 성격. HTTP 상태코드와의 매핑은 웹 계층(GlobalExceptionHandler)에서만 한다.
 */
public enum ErrorType {
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    BAD_GATEWAY
}
