package com.zhs.controller;



import com.zhs.service.IBasketStageStandingService;
import com.zhs.model.BasketStageStanding;
import com.zhs.dto.BasketStageStandingDto;

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
@RequestMapping("/basket/stage/standing")
@Api(value = "")
@Slf4j
public class BasketStageStandingController {

    @Resource
    private IBasketStageStandingService  iBasketStageStandingService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketStageStanding>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketStageStanding> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketStageStanding> lambda = new QueryWrapper<BasketStageStanding>().lambda();
        //此处可以拼条件
        lambda.eq(BasketStageStanding::getIsDelete,0);
        IPage<BasketStageStanding> pages =  iBasketStageStandingService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketStageStanding> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketStageStanding> wrapper = new QueryWrapper<BasketStageStanding>()
                            .lambda().eq(BasketStageStanding::getId,id).eq(BasketStageStanding::getIsDelete,0);
        return R.ok(iBasketStageStandingService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketStageStanding> query = new QueryWrapper<BasketStageStanding>().lambda().eq(BasketStageStanding::getId, id).eq(BasketStageStanding::getIsDelete, 0);
        BasketStageStanding basketStageStanding = iBasketStageStandingService.getOne(query);
        if(ObjectUtils.isEmpty(basketStageStanding)) throw new ServiceException("该数据不存在或者已经被删除");
        basketStageStanding.setIsDelete(1);
        iBasketStageStandingService.updateById(basketStageStanding);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketStageStandingDto basketStageStandingDto){
        if (CollectionUtils.isEmpty(basketStageStandingDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketStageStanding> list = new ArrayList<>();
        basketStageStandingDto.getIdList().stream().forEach(id ->{
        BasketStageStanding basketStageStanding = iBasketStageStandingService.getById(id);
            if (ObjectUtils.isEmpty(basketStageStanding)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketStageStanding.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketStageStanding.setIsDelete(1);
            list.add(basketStageStanding);
        });
        iBasketStageStandingService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketStageStandingDto basketStageStandingDto){
        return iBasketStageStandingService.addOrUpdate(basketStageStandingDto);
    }

}
