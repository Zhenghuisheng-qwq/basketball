package com.zhs.controller;



import com.zhs.service.IBasketWebLoginLogService;
import com.zhs.model.BasketWebLoginLog;
import com.zhs.dto.BasketWebLoginLogDto;

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
@RequestMapping("/basket/web/login/log")
@Api(value = "")
@Slf4j
public class BasketWebLoginLogController {

    @Resource
    private IBasketWebLoginLogService  iBasketWebLoginLogService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketWebLoginLog>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketWebLoginLog> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketWebLoginLog> lambda = new QueryWrapper<BasketWebLoginLog>().lambda();
        //此处可以拼条件
        lambda.eq(BasketWebLoginLog::getIsDelete,0);
        IPage<BasketWebLoginLog> pages =  iBasketWebLoginLogService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketWebLoginLog> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketWebLoginLog> wrapper = new QueryWrapper<BasketWebLoginLog>()
                            .lambda().eq(BasketWebLoginLog::getId,id).eq(BasketWebLoginLog::getIsDelete,0);
        return R.ok(iBasketWebLoginLogService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketWebLoginLog> query = new QueryWrapper<BasketWebLoginLog>().lambda().eq(BasketWebLoginLog::getId, id).eq(BasketWebLoginLog::getIsDelete, 0);
        BasketWebLoginLog basketWebLoginLog = iBasketWebLoginLogService.getOne(query);
        if(ObjectUtils.isEmpty(basketWebLoginLog)) throw new ServiceException("该数据不存在或者已经被删除");
        basketWebLoginLog.setIsDelete(1);
        iBasketWebLoginLogService.updateById(basketWebLoginLog);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketWebLoginLogDto basketWebLoginLogDto){
        if (CollectionUtils.isEmpty(basketWebLoginLogDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketWebLoginLog> list = new ArrayList<>();
        basketWebLoginLogDto.getIdList().stream().forEach(id ->{
        BasketWebLoginLog basketWebLoginLog = iBasketWebLoginLogService.getById(id);
            if (ObjectUtils.isEmpty(basketWebLoginLog)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketWebLoginLog.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketWebLoginLog.setIsDelete(1);
            list.add(basketWebLoginLog);
        });
        iBasketWebLoginLogService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketWebLoginLogDto basketWebLoginLogDto){
        return iBasketWebLoginLogService.addOrUpdate(basketWebLoginLogDto);
    }

}
