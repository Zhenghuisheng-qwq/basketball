package com.zhs.service.impl;


import com.zhs.model.BasketNotificationSetting;
import com.zhs.dao.BasketNotificationSettingDao;
import com.zhs.service.IBasketNotificationSettingService;
import com.zhs.dto.BasketNotificationSettingDto;


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
public class BasketNotificationSettingServiceImpl extends ServiceImpl< BasketNotificationSettingDao, BasketNotificationSetting> implements IBasketNotificationSettingService {

    @Resource
    private BasketNotificationSettingDao basketNotificationSettingDao;

    @Override
    public R addOrUpdate(BasketNotificationSettingDto basketNotificationSettingDto) {
        if (BeanUtil.isEmpty(basketNotificationSettingDto)) throw new ServiceException("数据不能为空");
        BasketNotificationSetting basketNotificationSetting = new BasketNotificationSetting();
        BeanUtil.copyProperties(basketNotificationSettingDto,basketNotificationSetting);
        if (basketNotificationSettingDto.getId() == null){
            basketNotificationSettingDao.insert(basketNotificationSetting);
            return R.ok("数据插入成功");
        }else {
            basketNotificationSettingDao.updateById(basketNotificationSetting);
            return R.ok("数据更新成功");
        }
    }
}