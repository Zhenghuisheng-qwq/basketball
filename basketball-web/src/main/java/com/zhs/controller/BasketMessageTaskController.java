package com.zhs.controller;



import com.zhs.service.IBasketMessageTaskService;
import com.zhs.model.BasketMessageTask;
import com.zhs.dto.BasketMessageTaskDto;

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
@RequestMapping("/basket/message/task")
@Api(value = "")
@Slf4j
public class BasketMessageTaskController {

    @Resource
    private IBasketMessageTaskService  iBasketMessageTaskService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketMessageTask>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketMessageTask> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketMessageTask> lambda = new QueryWrapper<BasketMessageTask>().lambda();
        //此处可以拼条件
        lambda.eq(BasketMessageTask::getIsDelete,0);
        IPage<BasketMessageTask> pages =  iBasketMessageTaskService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketMessageTask> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketMessageTask> wrapper = new QueryWrapper<BasketMessageTask>()
                            .lambda().eq(BasketMessageTask::getId,id).eq(BasketMessageTask::getIsDelete,0);
        return R.ok(iBasketMessageTaskService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketMessageTask> query = new QueryWrapper<BasketMessageTask>().lambda().eq(BasketMessageTask::getId, id).eq(BasketMessageTask::getIsDelete, 0);
        BasketMessageTask basketMessageTask = iBasketMessageTaskService.getOne(query);
        if(ObjectUtils.isEmpty(basketMessageTask)) throw new ServiceException("该数据不存在或者已经被删除");
        basketMessageTask.setIsDelete(1);
        iBasketMessageTaskService.updateById(basketMessageTask);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketMessageTaskDto basketMessageTaskDto){
        if (CollectionUtils.isEmpty(basketMessageTaskDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketMessageTask> list = new ArrayList<>();
        basketMessageTaskDto.getIdList().stream().forEach(id ->{
        BasketMessageTask basketMessageTask = iBasketMessageTaskService.getById(id);
            if (ObjectUtils.isEmpty(basketMessageTask)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketMessageTask.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketMessageTask.setIsDelete(1);
            list.add(basketMessageTask);
        });
        iBasketMessageTaskService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketMessageTaskDto basketMessageTaskDto){
        return iBasketMessageTaskService.addOrUpdate(basketMessageTaskDto);
    }

}
