package com.zhs.controller;



import com.zhs.service.IBasketSiteBannerService;
import com.zhs.model.BasketSiteBanner;
import com.zhs.dto.BasketSiteBannerDto;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.zhs.exception.ServiceException;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 
 * @since 2026-07-18
 */
@RestController
@RequestMapping("/basket/site/banner")
@Api(value = "")
@Slf4j
public class BasketSiteBannerController {

    @Resource
    private IBasketSiteBannerService  iBasketSiteBannerService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketSiteBanner>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketSiteBanner> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketSiteBanner> lambda = new QueryWrapper<BasketSiteBanner>().lambda();
        //此处可以拼条件
        lambda.eq(BasketSiteBanner::getIsDelete,0);
        IPage<BasketSiteBanner> pages =  iBasketSiteBannerService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketSiteBanner> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketSiteBanner> wrapper = new QueryWrapper<BasketSiteBanner>()
                            .lambda().eq(BasketSiteBanner::getId,id).eq(BasketSiteBanner::getIsDelete,0);
        return R.ok(iBasketSiteBannerService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketSiteBanner> query = new QueryWrapper<BasketSiteBanner>().lambda().eq(BasketSiteBanner::getId, id).eq(BasketSiteBanner::getIsDelete, 0);
        BasketSiteBanner basketSiteBanner = iBasketSiteBannerService.getOne(query);
        if(ObjectUtils.isEmpty(basketSiteBanner)) throw new ServiceException("该数据不存在或者已经被删除");
        basketSiteBanner.setIsDelete(1);
        iBasketSiteBannerService.updateById(basketSiteBanner);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketSiteBannerDto basketSiteBannerDto){
        if (CollectionUtils.isEmpty(basketSiteBannerDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketSiteBanner> list = new ArrayList<>();
        basketSiteBannerDto.getIdList().stream().forEach(id ->{
        BasketSiteBanner basketSiteBanner = iBasketSiteBannerService.getById(id);
            if (ObjectUtils.isEmpty(basketSiteBanner)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketSiteBanner.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketSiteBanner.setIsDelete(1);
            list.add(basketSiteBanner);
        });
        iBasketSiteBannerService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketSiteBannerDto basketSiteBannerDto){
        return iBasketSiteBannerService.addOrUpdate(basketSiteBannerDto);
    }

}
