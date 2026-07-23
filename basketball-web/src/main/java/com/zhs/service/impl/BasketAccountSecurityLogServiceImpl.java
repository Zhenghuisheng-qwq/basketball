package com.zhs.service.impl;


import com.zhs.model.BasketAccountSecurityLog;
import com.zhs.dao.BasketAccountSecurityLogDao;
import com.zhs.service.IBasketAccountSecurityLogService;
import com.zhs.dto.BasketAccountSecurityLogDto;


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
public class BasketAccountSecurityLogServiceImpl extends ServiceImpl< BasketAccountSecurityLogDao, BasketAccountSecurityLog> implements IBasketAccountSecurityLogService {

    @Resource
    private BasketAccountSecurityLogDao basketAccountSecurityLogDao;

    @Override
    public R addOrUpdate(BasketAccountSecurityLogDto basketAccountSecurityLogDto) {
        if (BeanUtil.isEmpty(basketAccountSecurityLogDto)) throw new ServiceException("数据不能为空");
        BasketAccountSecurityLog basketAccountSecurityLog = new BasketAccountSecurityLog();
        BeanUtil.copyProperties(basketAccountSecurityLogDto,basketAccountSecurityLog);
        if (basketAccountSecurityLogDto.getId() == null){
            basketAccountSecurityLogDao.insert(basketAccountSecurityLog);
            return R.ok("数据插入成功");
        }else {
            basketAccountSecurityLogDao.updateById(basketAccountSecurityLog);
            return R.ok("数据更新成功");
        }
    }
}