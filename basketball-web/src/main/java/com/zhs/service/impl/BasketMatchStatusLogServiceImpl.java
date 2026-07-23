package com.zhs.service.impl;


import com.zhs.model.BasketMatchStatusLog;
import com.zhs.dao.BasketMatchStatusLogDao;
import com.zhs.service.IBasketMatchStatusLogService;
import com.zhs.dto.BasketMatchStatusLogDto;


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
public class BasketMatchStatusLogServiceImpl extends ServiceImpl< BasketMatchStatusLogDao, BasketMatchStatusLog> implements IBasketMatchStatusLogService {

    @Resource
    private BasketMatchStatusLogDao basketMatchStatusLogDao;

    @Override
    public R addOrUpdate(BasketMatchStatusLogDto basketMatchStatusLogDto) {
        if (BeanUtil.isEmpty(basketMatchStatusLogDto)) throw new ServiceException("数据不能为空");
        BasketMatchStatusLog basketMatchStatusLog = new BasketMatchStatusLog();
        BeanUtil.copyProperties(basketMatchStatusLogDto,basketMatchStatusLog);
        if (basketMatchStatusLogDto.getId() == null){
            basketMatchStatusLogDao.insert(basketMatchStatusLog);
            return R.ok("数据插入成功");
        }else {
            basketMatchStatusLogDao.updateById(basketMatchStatusLog);
            return R.ok("数据更新成功");
        }
    }
}