package com.zhs.controller;



import com.zhs.service.IBasketPartnerService;
import com.zhs.model.BasketPartner;
import com.zhs.dto.BasketPartnerDto;

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
 * @since 2026-07-24
 */
@RestController
@RequestMapping("/basket/partner")
@Api(value = "")
@Slf4j
public class BasketPartnerController {

    @Resource
    private IBasketPartnerService  iBasketPartnerService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketPartner>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketPartner> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketPartner> lambda = new QueryWrapper<BasketPartner>().lambda();
        //此处可以拼条件
        lambda.eq(BasketPartner::getIsDelete,0);
        IPage<BasketPartner> pages =  iBasketPartnerService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketPartner> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketPartner> wrapper = new QueryWrapper<BasketPartner>()
                            .lambda().eq(BasketPartner::getId,id).eq(BasketPartner::getIsDelete,0);
        return R.ok(iBasketPartnerService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketPartner> query = new QueryWrapper<BasketPartner>().lambda().eq(BasketPartner::getId, id).eq(BasketPartner::getIsDelete, 0);
        BasketPartner basketPartner = iBasketPartnerService.getOne(query);
        if(ObjectUtils.isEmpty(basketPartner)) throw new ServiceException("该数据不存在或者已经被删除");
        basketPartner.setIsDelete(1);
        iBasketPartnerService.updateById(basketPartner);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketPartnerDto basketPartnerDto){
        if (CollectionUtils.isEmpty(basketPartnerDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketPartner> list = new ArrayList<>();
        basketPartnerDto.getIdList().stream().forEach(id ->{
        BasketPartner basketPartner = iBasketPartnerService.getById(id);
            if (ObjectUtils.isEmpty(basketPartner)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketPartner.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketPartner.setIsDelete(1);
            list.add(basketPartner);
        });
        iBasketPartnerService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketPartnerDto basketPartnerDto){
        return iBasketPartnerService.addOrUpdate(basketPartnerDto);
    }

}
