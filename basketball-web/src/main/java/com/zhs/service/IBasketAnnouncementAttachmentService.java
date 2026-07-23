package com.zhs.service;


import com.zhs.model.BasketAnnouncementAttachment;
import com.zhs.dto.BasketAnnouncementAttachmentDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketAnnouncementAttachmentService extends IService<BasketAnnouncementAttachment> {
    R addOrUpdate(BasketAnnouncementAttachmentDto basketAnnouncementAttachmentDto);
}
