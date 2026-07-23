package com.zhs.service;


import com.zhs.model.BasketAnnouncement;
import com.zhs.dto.BasketAnnouncementDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketAnnouncementService extends IService<BasketAnnouncement> {
    R addOrUpdate(BasketAnnouncementDto basketAnnouncementDto);
}
