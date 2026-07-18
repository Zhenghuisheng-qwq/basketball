package com.zhs.controller;



import com.zhs.service.IBasketTournamentPlayerService;
import com.zhs.model.BasketTournamentPlayer;
import com.zhs.dto.BasketTournamentPlayerDto;

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
@RequestMapping("/basket/tournament/player")
@Api(value = "")
@Slf4j
public class BasketTournamentPlayerController {

    @Resource
    private IBasketTournamentPlayerService  iBasketTournamentPlayerService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTournamentPlayer>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTournamentPlayer> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTournamentPlayer> lambda = new QueryWrapper<BasketTournamentPlayer>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTournamentPlayer::getIsDelete,0);
        IPage<BasketTournamentPlayer> pages =  iBasketTournamentPlayerService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTournamentPlayer> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTournamentPlayer> wrapper = new QueryWrapper<BasketTournamentPlayer>()
                            .lambda().eq(BasketTournamentPlayer::getId,id).eq(BasketTournamentPlayer::getIsDelete,0);
        return R.ok(iBasketTournamentPlayerService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTournamentPlayer> query = new QueryWrapper<BasketTournamentPlayer>().lambda().eq(BasketTournamentPlayer::getId, id).eq(BasketTournamentPlayer::getIsDelete, 0);
        BasketTournamentPlayer basketTournamentPlayer = iBasketTournamentPlayerService.getOne(query);
        if(ObjectUtils.isEmpty(basketTournamentPlayer)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTournamentPlayer.setIsDelete(1);
        iBasketTournamentPlayerService.updateById(basketTournamentPlayer);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTournamentPlayerDto basketTournamentPlayerDto){
        if (CollectionUtils.isEmpty(basketTournamentPlayerDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTournamentPlayer> list = new ArrayList<>();
        basketTournamentPlayerDto.getIdList().stream().forEach(id ->{
        BasketTournamentPlayer basketTournamentPlayer = iBasketTournamentPlayerService.getById(id);
            if (ObjectUtils.isEmpty(basketTournamentPlayer)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTournamentPlayer.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTournamentPlayer.setIsDelete(1);
            list.add(basketTournamentPlayer);
        });
        iBasketTournamentPlayerService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTournamentPlayerDto basketTournamentPlayerDto){
        return iBasketTournamentPlayerService.addOrUpdate(basketTournamentPlayerDto);
    }

}
