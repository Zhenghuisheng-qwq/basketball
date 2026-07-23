package com.zhs.service.impl;


import com.zhs.model.BasketMessageTask;
import com.zhs.dao.BasketMessageTaskDao;
import com.zhs.service.IBasketMessageTaskService;
import com.zhs.dto.BasketMessageTaskDto;


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
public class BasketMessageTaskServiceImpl extends ServiceImpl< BasketMessageTaskDao, BasketMessageTask> implements IBasketMessageTaskService {

    @Resource
    private BasketMessageTaskDao basketMessageTaskDao;

    @Override
    public R addOrUpdate(BasketMessageTaskDto basketMessageTaskDto) {
        if (BeanUtil.isEmpty(basketMessageTaskDto)) throw new ServiceException("数据不能为空");
        BasketMessageTask basketMessageTask = new BasketMessageTask();
        BeanUtil.copyProperties(basketMessageTaskDto,basketMessageTask);
        if (basketMessageTaskDto.getId() == null){
            basketMessageTaskDao.insert(basketMessageTask);
            return R.ok("数据插入成功");
        }else {
            basketMessageTaskDao.updateById(basketMessageTask);
            return R.ok("数据更新成功");
        }
    }
}