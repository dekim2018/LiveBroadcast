package com.livebroadcast.auth.in.web.dto;

/**
 * Refresh Token 은 응답 본문에 넣지 않고 HttpOnly 쿠키로만 내려준다(XSS 로 탈취되지 않도록).
 */
public record TokenResponse(String accessToken, long expiresIn, UserResponse user) {
}
