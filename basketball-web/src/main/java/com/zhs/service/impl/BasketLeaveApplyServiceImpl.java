package com.zhs.service.impl;


import com.zhs.model.BasketLeaveApply;
import com.zhs.dao.BasketLeaveApplyDao;
import com.zhs.service.IBasketLeaveApplyService;
import com.zhs.dto.BasketLeaveApplyDto;


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
public class BasketLeaveApplyServiceImpl extends ServiceImpl< BasketLeaveApplyDao, BasketLeaveApply> implements IBasketLeaveApplyService {

    @Resource
    private BasketLeaveApplyDao basketLeaveApplyDao;

    @Override
    public R addOrUpdate(BasketLeaveApplyDto basketLeaveApplyDto) {
        if (BeanUtil.isEmpty(basketLeaveApplyDto)) throw new ServiceException("数据不能为空");
        BasketLeaveApply basketLeaveApply = new BasketLeaveApply();
        BeanUtil.copyProperties(basketLeaveApplyDto,basketLeaveApply);
        if (basketLeaveApplyDto.getId() == null){
            basketLeaveApplyDao.insert(basketLeaveApply);
            return R.ok("数据插入成功");
        }else {
            basketLeaveApplyDao.updateById(basketLeaveApply);
            return R.ok("数据更新成功");
        }
    }
}