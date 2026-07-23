package com.zhs.service.impl;


import com.zhs.model.BasketAnnouncement;
import com.zhs.dao.BasketAnnouncementDao;
import com.zhs.service.IBasketAnnouncementService;
import com.zhs.dto.BasketAnnouncementDto;


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
public class BasketAnnouncementServiceImpl extends ServiceImpl< BasketAnnouncementDao, BasketAnnouncement> implements IBasketAnnouncementService {

    @Resource
    private BasketAnnouncementDao basketAnnouncementDao;

    @Override
    public R addOrUpdate(BasketAnnouncementDto basketAnnouncementDto) {
        if (BeanUtil.isEmpty(basketAnnouncementDto)) throw new ServiceException("数据不能为空");
        BasketAnnouncement basketAnnouncement = new BasketAnnouncement();
        BeanUtil.copyProperties(basketAnnouncementDto,basketAnnouncement);
        if (basketAnnouncementDto.getId() == null){
            basketAnnouncementDao.insert(basketAnnouncement);
            return R.ok("数据插入成功");
        }else {
            basketAnnouncementDao.updateById(basketAnnouncement);
            return R.ok("数据更新成功");
        }
    }
}