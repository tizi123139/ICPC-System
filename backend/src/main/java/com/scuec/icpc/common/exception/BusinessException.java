package com.scuec.icpc.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常。code 默认与 HTTP 状态码保持一致，便于前端统一处理。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus status;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, 400, message);
    }

    public BusinessException(HttpStatus status, String message) {
        this(status, status.value(), message);
    }

    public BusinessException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }

    public static BusinessException unprocessable(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message);
    }
}
