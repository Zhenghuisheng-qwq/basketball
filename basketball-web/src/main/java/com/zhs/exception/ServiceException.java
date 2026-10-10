package com.zhs.exception;

/**
 * 业务异常（支持自定义 code）
 */
public final class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * HTTP / 业务数字码，保持原有 400/401/403 语义。
     */
    private Integer code;

    /**
     * 稳定业务原因码。旧客户端可忽略；前端按此选择文案。
     */
    private String reasonCode;

    /**
     * 错误提示（中文，兼容旧客户端）
     */
    private String message;

    /**
     * 错误明细，内部调试错误
     */
    private String detailMessage;

    /**
     * 空构造方法，避免反序列化问题
     */
    public ServiceException() {
    }

    public ServiceException(String message) {
        this.message = message;
    }

    public ServiceException(String message, Integer code) {
        this.message = message;
        this.code = code;
    }

    public ServiceException(String message, String reasonCode) {
        this.message = message;
        this.reasonCode = reasonCode;
    }

    public ServiceException(String message, Integer code, String reasonCode) {
        this.message = message;
        this.code = code;
        this.reasonCode = reasonCode;
    }

    public Integer getCode() {
        return code;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public ServiceException setMessage(String message) {
        this.message = message;
        return this;
    }

    public ServiceException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }
}
