package com.zhs.controller;



import com.zhs.service.IBasketRoundService;
import com.zhs.model.BasketRound;
import com.zhs.dto.BasketRoundDto;

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
@RequestMapping("/basket/round")
@Api(value = "")
@Slf4j
public class BasketRoundController {

    @Resource
    private IBasketRoundService  iBasketRoundService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketRound>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketRound> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketRound> lambda = new QueryWrapper<BasketRound>().lambda();
        //此处可以拼条件
        lambda.eq(BasketRound::getIsDelete,0);
        IPage<BasketRound> pages =  iBasketRoundService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketRound> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketRound> wrapper = new QueryWrapper<BasketRound>()
                            .lambda().eq(BasketRound::getId,id).eq(BasketRound::getIsDelete,0);
        return R.ok(iBasketRoundService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketRound> query = new QueryWrapper<BasketRound>().lambda().eq(BasketRound::getId, id).eq(BasketRound::getIsDelete, 0);
        BasketRound basketRound = iBasketRoundService.getOne(query);
        if(ObjectUtils.isEmpty(basketRound)) throw new ServiceException("该数据不存在或者已经被删除");
        basketRound.setIsDelete(1);
        iBasketRoundService.updateById(basketRound);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketRoundDto basketRoundDto){
        if (CollectionUtils.isEmpty(basketRoundDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketRound> list = new ArrayList<>();
        basketRoundDto.getIdList().stream().forEach(id ->{
        BasketRound basketRound = iBasketRoundService.getById(id);
            if (ObjectUtils.isEmpty(basketRound)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketRound.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketRound.setIsDelete(1);
            list.add(basketRound);
        });
        iBasketRoundService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketRoundDto basketRoundDto){
        return iBasketRoundService.addOrUpdate(basketRoundDto);
    }

}
