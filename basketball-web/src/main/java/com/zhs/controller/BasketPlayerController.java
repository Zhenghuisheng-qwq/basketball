package com.zhs.controller;



import com.zhs.service.IBasketPlayerService;
import com.zhs.model.BasketPlayer;
import com.zhs.dto.BasketPlayerDto;

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
@RequestMapping("/basket/player")
@Api(value = "")
@Slf4j
public class BasketPlayerController {

    @Resource
    private IBasketPlayerService  iBasketPlayerService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketPlayer>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketPlayer> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketPlayer> lambda = new QueryWrapper<BasketPlayer>().lambda();
        //此处可以拼条件
        lambda.eq(BasketPlayer::getIsDelete,0);
        IPage<BasketPlayer> pages =  iBasketPlayerService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketPlayer> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketPlayer> wrapper = new QueryWrapper<BasketPlayer>()
                            .lambda().eq(BasketPlayer::getId,id).eq(BasketPlayer::getIsDelete,0);
        return R.ok(iBasketPlayerService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketPlayer> query = new QueryWrapper<BasketPlayer>().lambda().eq(BasketPlayer::getId, id).eq(BasketPlayer::getIsDelete, 0);
        BasketPlayer basketPlayer = iBasketPlayerService.getOne(query);
        if(ObjectUtils.isEmpty(basketPlayer)) throw new ServiceException("该数据不存在或者已经被删除");
        basketPlayer.setIsDelete(1);
        iBasketPlayerService.updateById(basketPlayer);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketPlayerDto basketPlayerDto){
        if (CollectionUtils.isEmpty(basketPlayerDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketPlayer> list = new ArrayList<>();
        basketPlayerDto.getIdList().stream().forEach(id ->{
        BasketPlayer basketPlayer = iBasketPlayerService.getById(id);
            if (ObjectUtils.isEmpty(basketPlayer)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketPlayer.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketPlayer.setIsDelete(1);
            list.add(basketPlayer);
        });
        iBasketPlayerService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketPlayerDto basketPlayerDto){
        return iBasketPlayerService.addOrUpdate(basketPlayerDto);
    }

}
