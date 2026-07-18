package com.zhs.service.impl;


import com.zhs.model.BasketCaptainApply;
import com.zhs.dao.BasketCaptainApplyDao;
import com.zhs.service.IBasketCaptainApplyService;
import com.zhs.dto.BasketCaptainApplyDto;


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
public class BasketCaptainApplyServiceImpl extends ServiceImpl< BasketCaptainApplyDao, BasketCaptainApply> implements IBasketCaptainApplyService {

    @Resource
    private BasketCaptainApplyDao basketCaptainApplyDao;

    @Override
    public R addOrUpdate(BasketCaptainApplyDto basketCaptainApplyDto) {
        if (BeanUtil.isEmpty(basketCaptainApplyDto)) throw new ServiceException("数据不能为空");
        BasketCaptainApply basketCaptainApply = new BasketCaptainApply();
        BeanUtil.copyProperties(basketCaptainApplyDto,basketCaptainApply);
        if (basketCaptainApplyDto.getId() == null){
            basketCaptainApplyDao.insert(basketCaptainApply);
            return R.ok("数据插入成功");
        }else {
            basketCaptainApplyDao.updateById(basketCaptainApply);
            return R.ok("数据更新成功");
        }
    }
}