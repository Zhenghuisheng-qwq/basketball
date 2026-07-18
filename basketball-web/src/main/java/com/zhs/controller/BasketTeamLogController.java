package com.zhs.controller;



import com.zhs.service.IBasketTeamLogService;
import com.zhs.model.BasketTeamLog;
import com.zhs.dto.BasketTeamLogDto;

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
@RequestMapping("/basket/team/log")
@Api(value = "")
@Slf4j
public class BasketTeamLogController {

    @Resource
    private IBasketTeamLogService  iBasketTeamLogService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTeamLog>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTeamLog> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTeamLog> lambda = new QueryWrapper<BasketTeamLog>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTeamLog::getIsDelete,0);
        IPage<BasketTeamLog> pages =  iBasketTeamLogService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTeamLog> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTeamLog> wrapper = new QueryWrapper<BasketTeamLog>()
                            .lambda().eq(BasketTeamLog::getId,id).eq(BasketTeamLog::getIsDelete,0);
        return R.ok(iBasketTeamLogService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTeamLog> query = new QueryWrapper<BasketTeamLog>().lambda().eq(BasketTeamLog::getId, id).eq(BasketTeamLog::getIsDelete, 0);
        BasketTeamLog basketTeamLog = iBasketTeamLogService.getOne(query);
        if(ObjectUtils.isEmpty(basketTeamLog)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTeamLog.setIsDelete(1);
        iBasketTeamLogService.updateById(basketTeamLog);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTeamLogDto basketTeamLogDto){
        if (CollectionUtils.isEmpty(basketTeamLogDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTeamLog> list = new ArrayList<>();
        basketTeamLogDto.getIdList().stream().forEach(id ->{
        BasketTeamLog basketTeamLog = iBasketTeamLogService.getById(id);
            if (ObjectUtils.isEmpty(basketTeamLog)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTeamLog.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTeamLog.setIsDelete(1);
            list.add(basketTeamLog);
        });
        iBasketTeamLogService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTeamLogDto basketTeamLogDto){
        return iBasketTeamLogService.addOrUpdate(basketTeamLogDto);
    }

}
