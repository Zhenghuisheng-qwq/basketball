package com.zhs.service.impl;


import com.zhs.model.BasketPlayerProfile;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.service.IBasketPlayerProfileService;
import com.zhs.dto.BasketPlayerProfileDto;


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
public class BasketPlayerProfileServiceImpl extends ServiceImpl< BasketPlayerProfileDao, BasketPlayerProfile> implements IBasketPlayerProfileService {

    @Resource
    private BasketPlayerProfileDao basketPlayerProfileDao;

    @Override
    public R addOrUpdate(BasketPlayerProfileDto basketPlayerProfileDto) {
        if (BeanUtil.isEmpty(basketPlayerProfileDto)) throw new ServiceException("数据不能为空");
        BasketPlayerProfile basketPlayerProfile = new BasketPlayerProfile();
        BeanUtil.copyProperties(basketPlayerProfileDto,basketPlayerProfile);
        if (basketPlayerProfileDto.getId() == null){
            basketPlayerProfileDao.insert(basketPlayerProfile);
            return R.ok("数据插入成功");
        }else {
            basketPlayerProfileDao.updateById(basketPlayerProfile);
            return R.ok("数据更新成功");
        }
    }
}