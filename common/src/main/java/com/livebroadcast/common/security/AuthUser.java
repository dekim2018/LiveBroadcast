package com.livebroadcast.common.security;

/**
 * 인증된 요청의 사용자 정보. JWT 의 sub(userId)와 nickname 클레임에서 만든다.
 */
public record AuthUser(Long id, String nickname) {
}
