package com.zhs.service.impl;


import com.zhs.model.BasketFavorite;
import com.zhs.dao.BasketFavoriteDao;
import com.zhs.service.IBasketFavoriteService;
import com.zhs.dto.BasketFavoriteDto;


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
public class BasketFavoriteServiceImpl extends ServiceImpl< BasketFavoriteDao, BasketFavorite> implements IBasketFavoriteService {

    @Resource
    private BasketFavoriteDao basketFavoriteDao;

    @Override
    public R addOrUpdate(BasketFavoriteDto basketFavoriteDto) {
        if (BeanUtil.isEmpty(basketFavoriteDto)) throw new ServiceException("数据不能为空");
        BasketFavorite basketFavorite = new BasketFavorite();
        BeanUtil.copyProperties(basketFavoriteDto,basketFavorite);
        if (basketFavoriteDto.getId() == null){
            basketFavoriteDao.insert(basketFavorite);
            return R.ok("数据插入成功");
        }else {
            basketFavoriteDao.updateById(basketFavorite);
            return R.ok("数据更新成功");
        }
    }
}