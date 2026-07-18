package com.zhs.service.impl;


import com.zhs.model.BasketEmailCode;
import com.zhs.dao.BasketEmailCodeDao;
import com.zhs.service.IBasketEmailCodeService;
import com.zhs.dto.BasketEmailCodeDto;


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
public class BasketEmailCodeServiceImpl extends ServiceImpl< BasketEmailCodeDao, BasketEmailCode> implements IBasketEmailCodeService {

    @Resource
    private BasketEmailCodeDao basketEmailCodeDao;

    @Override
    public R addOrUpdate(BasketEmailCodeDto basketEmailCodeDto) {
        if (BeanUtil.isEmpty(basketEmailCodeDto)) throw new ServiceException("数据不能为空");
        BasketEmailCode basketEmailCode = new BasketEmailCode();
        BeanUtil.copyProperties(basketEmailCodeDto,basketEmailCode);
        if (basketEmailCodeDto.getId() == null){
            basketEmailCodeDao.insert(basketEmailCode);
            return R.ok("数据插入成功");
        }else {
            basketEmailCodeDao.updateById(basketEmailCode);
            return R.ok("数据更新成功");
        }
    }
}