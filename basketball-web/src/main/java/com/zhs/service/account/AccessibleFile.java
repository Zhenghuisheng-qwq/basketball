package com.zhs.service.account;

import com.zhs.model.BasketFileAsset;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class AccessibleFile {

    private BasketFileAsset asset;

    private byte[] content;

    private String redirectUrl;

    public boolean isRedirect() {
        return StringUtils.hasText(redirectUrl);
    }
}
