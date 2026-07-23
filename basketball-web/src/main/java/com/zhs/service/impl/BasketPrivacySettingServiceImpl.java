package com.zhs.service.impl;


import com.zhs.model.BasketPrivacySetting;
import com.zhs.dao.BasketPrivacySettingDao;
import com.zhs.service.IBasketPrivacySettingService;
import com.zhs.dto.BasketPrivacySettingDto;


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
public class BasketPrivacySettingServiceImpl extends ServiceImpl< BasketPrivacySettingDao, BasketPrivacySetting> implements IBasketPrivacySettingService {

    @Resource
    private BasketPrivacySettingDao basketPrivacySettingDao;

    @Override
    public R addOrUpdate(BasketPrivacySettingDto basketPrivacySettingDto) {
        if (BeanUtil.isEmpty(basketPrivacySettingDto)) throw new ServiceException("数据不能为空");
        BasketPrivacySetting basketPrivacySetting = new BasketPrivacySetting();
        BeanUtil.copyProperties(basketPrivacySettingDto,basketPrivacySetting);
        if (basketPrivacySettingDto.getId() == null){
            basketPrivacySettingDao.insert(basketPrivacySetting);
            return R.ok("数据插入成功");
        }else {
            basketPrivacySettingDao.updateById(basketPrivacySetting);
            return R.ok("数据更新成功");
        }
    }
}