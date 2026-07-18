package com.zhs.service.impl;


import com.zhs.model.BasketJoinApply;
import com.zhs.dao.BasketJoinApplyDao;
import com.zhs.service.IBasketJoinApplyService;
import com.zhs.dto.BasketJoinApplyDto;


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
public class BasketJoinApplyServiceImpl extends ServiceImpl< BasketJoinApplyDao, BasketJoinApply> implements IBasketJoinApplyService {

    @Resource
    private BasketJoinApplyDao basketJoinApplyDao;

    @Override
    public R addOrUpdate(BasketJoinApplyDto basketJoinApplyDto) {
        if (BeanUtil.isEmpty(basketJoinApplyDto)) throw new ServiceException("数据不能为空");
        BasketJoinApply basketJoinApply = new BasketJoinApply();
        BeanUtil.copyProperties(basketJoinApplyDto,basketJoinApply);
        if (basketJoinApplyDto.getId() == null){
            basketJoinApplyDao.insert(basketJoinApply);
            return R.ok("数据插入成功");
        }else {
            basketJoinApplyDao.updateById(basketJoinApply);
            return R.ok("数据更新成功");
        }
    }
}