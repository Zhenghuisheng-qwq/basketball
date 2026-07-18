package com.zhs.service.impl;


import com.zhs.model.BasketTransferApply;
import com.zhs.dao.BasketTransferApplyDao;
import com.zhs.service.IBasketTransferApplyService;
import com.zhs.dto.BasketTransferApplyDto;


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
public class BasketTransferApplyServiceImpl extends ServiceImpl< BasketTransferApplyDao, BasketTransferApply> implements IBasketTransferApplyService {

    @Resource
    private BasketTransferApplyDao basketTransferApplyDao;

    @Override
    public R addOrUpdate(BasketTransferApplyDto basketTransferApplyDto) {
        if (BeanUtil.isEmpty(basketTransferApplyDto)) throw new ServiceException("数据不能为空");
        BasketTransferApply basketTransferApply = new BasketTransferApply();
        BeanUtil.copyProperties(basketTransferApplyDto,basketTransferApply);
        if (basketTransferApplyDto.getId() == null){
            basketTransferApplyDao.insert(basketTransferApply);
            return R.ok("数据插入成功");
        }else {
            basketTransferApplyDao.updateById(basketTransferApply);
            return R.ok("数据更新成功");
        }
    }
}