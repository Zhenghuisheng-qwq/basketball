package com.zhs.service;


import com.zhs.model.BasketNotificationSetting;
import com.zhs.dto.BasketNotificationSettingDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketNotificationSettingService extends IService<BasketNotificationSetting> {
    R addOrUpdate(BasketNotificationSettingDto basketNotificationSettingDto);
}
