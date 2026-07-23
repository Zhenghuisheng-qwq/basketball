package com.zhs.controller;



import com.zhs.service.IBasketMatchStatusLogService;
import com.zhs.model.BasketMatchStatusLog;
import com.zhs.dto.BasketMatchStatusLogDto;

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
@RequestMapping("/basket/match/status/log")
@Api(value = "")
@Slf4j
public class BasketMatchStatusLogController {

    @Resource
    private IBasketMatchStatusLogService  iBasketMatchStatusLogService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketMatchStatusLog>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketMatchStatusLog> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketMatchStatusLog> lambda = new QueryWrapper<BasketMatchStatusLog>().lambda();
        //此处可以拼条件
        lambda.eq(BasketMatchStatusLog::getIsDelete,0);
        IPage<BasketMatchStatusLog> pages =  iBasketMatchStatusLogService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketMatchStatusLog> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketMatchStatusLog> wrapper = new QueryWrapper<BasketMatchStatusLog>()
                            .lambda().eq(BasketMatchStatusLog::getId,id).eq(BasketMatchStatusLog::getIsDelete,0);
        return R.ok(iBasketMatchStatusLogService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketMatchStatusLog> query = new QueryWrapper<BasketMatchStatusLog>().lambda().eq(BasketMatchStatusLog::getId, id).eq(BasketMatchStatusLog::getIsDelete, 0);
        BasketMatchStatusLog basketMatchStatusLog = iBasketMatchStatusLogService.getOne(query);
        if(ObjectUtils.isEmpty(basketMatchStatusLog)) throw new ServiceException("该数据不存在或者已经被删除");
        basketMatchStatusLog.setIsDelete(1);
        iBasketMatchStatusLogService.updateById(basketMatchStatusLog);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketMatchStatusLogDto basketMatchStatusLogDto){
        if (CollectionUtils.isEmpty(basketMatchStatusLogDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketMatchStatusLog> list = new ArrayList<>();
        basketMatchStatusLogDto.getIdList().stream().forEach(id ->{
        BasketMatchStatusLog basketMatchStatusLog = iBasketMatchStatusLogService.getById(id);
            if (ObjectUtils.isEmpty(basketMatchStatusLog)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketMatchStatusLog.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketMatchStatusLog.setIsDelete(1);
            list.add(basketMatchStatusLog);
        });
        iBasketMatchStatusLogService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketMatchStatusLogDto basketMatchStatusLogDto){
        return iBasketMatchStatusLogService.addOrUpdate(basketMatchStatusLogDto);
    }

}
