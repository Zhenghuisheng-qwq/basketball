package com.zhs.service.crypto;

/**
 * 敏感字段加解密（如证件号码）
 */
public interface IFieldEncryptService {

    /**
     * 加密明文；空值返回 null
     */
    String encrypt(String plaintext);

    /**
     * 解密密文；空值返回 null
     */
    String decrypt(String ciphertext);
}
