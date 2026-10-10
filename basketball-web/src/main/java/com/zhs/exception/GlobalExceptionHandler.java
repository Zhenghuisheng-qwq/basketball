package com.zhs.exception;

import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理，统一返回 R（code/msg/data）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<R<?>> handleServiceException(ServiceException e) {
        Integer code = e.getCode();
        int bodyCode = code == null ? 400 : code;
        R<?> body = R.fail(bodyCode, e.getMessage());
        body.setReasonCode(e.getReasonCode());
        if (code != null && code == 401) {
            return ResponseEntity.status(401).body(body);
        }
        if (code != null && code == 403) {
            return ResponseEntity.status(403).body(body);
        }
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<R<?>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过大小限制: {}", e.getMessage());
        return ResponseEntity.ok(R.fail("上传文件过大，单张图片不能超过 5MB"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<?>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.ok(R.fail("系统异常，请稍后重试"));
    }
}
