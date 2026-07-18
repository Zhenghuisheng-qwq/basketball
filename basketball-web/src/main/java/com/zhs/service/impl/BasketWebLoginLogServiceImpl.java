package com.zhs.service.impl;


import com.zhs.model.BasketWebLoginLog;
import com.zhs.dao.BasketWebLoginLogDao;
import com.zhs.service.IBasketWebLoginLogService;
import com.zhs.dto.BasketWebLoginLogDto;


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
 * @since 2026-07-18
 */
@Service
@Slf4j
public class BasketWebLoginLogServiceImpl extends ServiceImpl< BasketWebLoginLogDao, BasketWebLoginLog> implements IBasketWebLoginLogService {

    @Resource
    private BasketWebLoginLogDao basketWebLoginLogDao;

    @Override
    public R addOrUpdate(BasketWebLoginLogDto basketWebLoginLogDto) {
        if (BeanUtil.isEmpty(basketWebLoginLogDto)) throw new ServiceException("数据不能为空");
        BasketWebLoginLog basketWebLoginLog = new BasketWebLoginLog();
        BeanUtil.copyProperties(basketWebLoginLogDto,basketWebLoginLog);
        if (basketWebLoginLogDto.getId() == null){
            basketWebLoginLogDao.insert(basketWebLoginLog);
            return R.ok("数据插入成功");
        }else {
            basketWebLoginLogDao.updateById(basketWebLoginLog);
            return R.ok("数据更新成功");
        }
    }
}