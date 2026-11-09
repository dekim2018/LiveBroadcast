package com.livebroadcast.common.error;

/**
 * 모든 서비스의 도메인/애플리케이션 예외가 상속하는 기본 예외.
 * 스프링 의존성이 없어서 domain 패키지에서도 사용할 수 있다.
 */
public class DomainException extends RuntimeException {

    private final ErrorType type;
    private final String code;

    public DomainException(ErrorType type, String code, String message) {
        super(message);
        this.type = type;
        this.code = code;
    }

    public ErrorType getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public static DomainException badRequest(String code, String message) {
        return new DomainException(ErrorType.BAD_REQUEST, code, message);
    }

    public static DomainException unauthorized(String code, String message) {
        return new DomainException(ErrorType.UNAUTHORIZED, code, message);
    }

    public static DomainException forbidden(String code, String message) {
        return new DomainException(ErrorType.FORBIDDEN, code, message);
    }

    public static DomainException notFound(String code, String message) {
        return new DomainException(ErrorType.NOT_FOUND, code, message);
    }

    public static DomainException conflict(String code, String message) {
        return new DomainException(ErrorType.CONFLICT, code, message);
    }
}
