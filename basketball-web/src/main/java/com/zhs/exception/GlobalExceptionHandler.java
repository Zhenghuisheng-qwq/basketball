package com.zhs.exception;

import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理，统一返回 R（code/msg/data）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<R<?>> handleServiceException(ServiceException e) {
        Integer code = e.getCode();
        if (code != null && code == 401) {
            return ResponseEntity.status(401).body(R.fail(401, e.getMessage()));
        }
        if (code != null && code == 403) {
            return ResponseEntity.status(403).body(R.fail(403, e.getMessage()));
        }
        if (code != null) {
            return ResponseEntity.ok(R.fail(code, e.getMessage()));
        }
        return ResponseEntity.ok(R.fail(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<?>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.ok(R.fail("系统异常，请稍后重试"));
    }
}
