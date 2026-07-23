package com.zhs.controller;



import com.zhs.service.IBasketTournamentGroupService;
import com.zhs.model.BasketTournamentGroup;
import com.zhs.dto.BasketTournamentGroupDto;

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
@RequestMapping("/basket/tournament/group")
@Api(value = "")
@Slf4j
public class BasketTournamentGroupController {

    @Resource
    private IBasketTournamentGroupService  iBasketTournamentGroupService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTournamentGroup>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTournamentGroup> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTournamentGroup> lambda = new QueryWrapper<BasketTournamentGroup>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTournamentGroup::getIsDelete,0);
        IPage<BasketTournamentGroup> pages =  iBasketTournamentGroupService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTournamentGroup> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTournamentGroup> wrapper = new QueryWrapper<BasketTournamentGroup>()
                            .lambda().eq(BasketTournamentGroup::getId,id).eq(BasketTournamentGroup::getIsDelete,0);
        return R.ok(iBasketTournamentGroupService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTournamentGroup> query = new QueryWrapper<BasketTournamentGroup>().lambda().eq(BasketTournamentGroup::getId, id).eq(BasketTournamentGroup::getIsDelete, 0);
        BasketTournamentGroup basketTournamentGroup = iBasketTournamentGroupService.getOne(query);
        if(ObjectUtils.isEmpty(basketTournamentGroup)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTournamentGroup.setIsDelete(1);
        iBasketTournamentGroupService.updateById(basketTournamentGroup);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTournamentGroupDto basketTournamentGroupDto){
        if (CollectionUtils.isEmpty(basketTournamentGroupDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTournamentGroup> list = new ArrayList<>();
        basketTournamentGroupDto.getIdList().stream().forEach(id ->{
        BasketTournamentGroup basketTournamentGroup = iBasketTournamentGroupService.getById(id);
            if (ObjectUtils.isEmpty(basketTournamentGroup)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTournamentGroup.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTournamentGroup.setIsDelete(1);
            list.add(basketTournamentGroup);
        });
        iBasketTournamentGroupService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTournamentGroupDto basketTournamentGroupDto){
        return iBasketTournamentGroupService.addOrUpdate(basketTournamentGroupDto);
    }

}
