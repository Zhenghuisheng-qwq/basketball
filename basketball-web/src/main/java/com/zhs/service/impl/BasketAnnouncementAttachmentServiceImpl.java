package com.zhs.service.impl;


import com.zhs.model.BasketAnnouncementAttachment;
import com.zhs.dao.BasketAnnouncementAttachmentDao;
import com.zhs.service.IBasketAnnouncementAttachmentService;
import com.zhs.dto.BasketAnnouncementAttachmentDto;


import com.zhs.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.Date;


/**
 *
 * @author 
 * @since 2026-07-24
 */
@Service
@Slf4j
public class BasketAnnouncementAttachmentServiceImpl extends ServiceImpl< BasketAnnouncementAttachmentDao, BasketAnnouncementAttachment> implements IBasketAnnouncementAttachmentService {

    @Resource
    private BasketAnnouncementAttachmentDao basketAnnouncementAttachmentDao;

    @Override
    public R addOrUpdate(BasketAnnouncementAttachmentDto basketAnnouncementAttachmentDto) {
        if (BeanUtil.isEmpty(basketAnnouncementAttachmentDto)) throw new ServiceException("数据不能为空");
        BasketAnnouncementAttachment basketAnnouncementAttachment = new BasketAnnouncementAttachment();
        BeanUtil.copyProperties(basketAnnouncementAttachmentDto,basketAnnouncementAttachment);
        if (basketAnnouncementAttachmentDto.getId() == null){
            basketAnnouncementAttachmentDao.insert(basketAnnouncementAttachment);
            return R.ok("数据插入成功");
        }else {
            basketAnnouncementAttachmentDao.updateById(basketAnnouncementAttachment);
            return R.ok("数据更新成功");
        }
    }
}