package com.zhs.service;


import com.zhs.model.BasketPrivacySetting;
import com.zhs.dto.BasketPrivacySettingDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketPrivacySettingService extends IService<BasketPrivacySetting> {
    R addOrUpdate(BasketPrivacySettingDto basketPrivacySettingDto);
}
