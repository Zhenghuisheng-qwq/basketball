package com.zhs.service.crypto;

import cn.hutool.core.codec.Base64;
import com.zhs.config.AccountCryptoProperties;
import com.zhs.exception.ServiceException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class FieldEncryptServiceImpl implements IFieldEncryptService {

    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String ALGORITHM = "AES";

    @Resource
    private AccountCryptoProperties accountCryptoProperties;

    @Override
    public String encrypt(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            return null;
        }
        return Base64.encode(encryptBytes(plaintext.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String decrypt(String ciphertext) {
        if (!StringUtils.hasText(ciphertext)) {
            return null;
        }
        try {
            return new String(decryptBytes(Base64.decode(ciphertext)), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("字段解密失败");
        }
    }

    private byte[] encryptBytes(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, resolveSecretKey());
            return cipher.doFinal(data);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("字段加密失败");
        }
    }

    private byte[] decryptBytes(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, resolveSecretKey());
            return cipher.doFinal(data);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("字段解密失败");
        }
    }

    private SecretKeySpec resolveSecretKey() {
        byte[] keyBytes = resolveKeyBytes();
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new ServiceException("字段加密密钥长度无效");
        }
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    private byte[] resolveKeyBytes() {
        String configuredKey = accountCryptoProperties.getAesKey();
        if (!StringUtils.hasText(configuredKey)) {
            throw new ServiceException("字段加密密钥未配置");
        }
        String trimmed = configuredKey.trim();
        try {
            byte[] decoded = Base64.decode(trimmed);
            if (decoded != null && decoded.length > 0) {
                return decoded;
            }
        } catch (Exception ignored) {
            // 非 Base64 时按 UTF-8 原文处理
        }
        return trimmed.getBytes(StandardCharsets.UTF_8);
    }
}
