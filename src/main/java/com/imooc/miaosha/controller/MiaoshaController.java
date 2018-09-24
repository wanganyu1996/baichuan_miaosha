package com.imooc.miaosha.controller;

import com.imooc.miaosha.rabbitmq.MQSender;
import com.imooc.miaosha.rabbitmq.MiaoshaMessage;
import com.imooc.miaosha.redis.AccessKey;
import com.imooc.miaosha.redis.GoodsKey;
import com.imooc.miaosha.redis.MiaoshaKey;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.util.MD5Util;
import com.imooc.miaosha.util.UUIDUtil;
import com.rabbitmq.client.AMQP;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.imooc.miaosha.domain.MiaoshaOrder;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.domain.OrderInfo;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.service.GoodsService;
import com.imooc.miaosha.service.MiaoshaService;
import com.imooc.miaosha.service.MiaoshaUserService;
import com.imooc.miaosha.service.OrderService;
import com.imooc.miaosha.vo.GoodsVo;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;

    @Autowired
    MQSender sender;

    private Map<Long,Boolean> localOverMap=new HashMap<Long, Boolean>();

    @RequestMapping(value = "/{path}/do_miaosha", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser user,
                                   @RequestParam("goodsId") long goodsId, @PathVariable("path")String path) {

        model.addAttribute("user", user);
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        //验证Path
       boolean check= miaoshaService.checkPath(user,goodsId,path);
       if(!check){
           return Result.error(CodeMsg.REQUEST_ILLEGAL);
       }
        //内存标记
        boolean over=localOverMap.get(goodsId);
        if(over){
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }

        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);
        if (stock < 0) {
            localOverMap.put(goodsId,true);
            Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        //判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if (order != null) {
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }
        //入队
        MiaoshaMessage miaoshaMessage = new MiaoshaMessage();
        miaoshaMessage.setUser(user);
        miaoshaMessage.setGoodsId(goodsId);
        sender.sendMiaoshaMessage(miaoshaMessage);
        return Result.success(0);
    	/*
    	//判断库存
    	GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
    	int stock = goods.getStockCount();
    	if(stock <= 0) {
			return Result.error(CodeMsg.MIAO_SHA_OVER);
    	}
    	//判断是否已经秒杀到了
    	MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
    	if(order != null) {
    		return Result.error(CodeMsg.REPEATE_MIAOSHA);
    	}
    	//减库存 下订单 写入秒杀订单
    	OrderInfo orderInfo = miaoshaService.miaosha(user, goods);
    	*/


    }

    /**
     * orderId:成功
     * -1：秒杀失败
     * 0： 排队中
     *
     */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaReult(Model model, MiaoshaUser user,
                                @RequestParam("goodsId") long goodsId) {
        model.addAttribute("user", user);
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        long result=miaoshaService.getMiaoshaReult(user.getId(),goodsId);
        return Result.success(result);
    }

    /**
     * 系统初始化 加载商品数量到缓存
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsVoList = goodsService.listGoodsVo();
        if (goodsVoList == null) {
            return;
        }
        for (GoodsVo goods : goodsVoList) {
            redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goods.getId(), goods.getStockCount());
            localOverMap.put(goods.getId(),false);
        }
    }
    @RequestMapping(value = "/path", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(HttpServletRequest request,MiaoshaUser user,
                                         @RequestParam("goodsId") long goodsId, @RequestParam(value = "verifyCode",defaultValue = "0") int verifyCode) {
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        //查询访问的次数，限流
        String uri=request.getRequestURI();
        String key=uri+"_"+user.getId();
        Integer count=redisService.get(AccessKey.access,""+key,Integer.class);
        if(count==null){
            redisService.set(AccessKey.access,key,1);
        }else if(count<5){
           redisService.incr(AccessKey.access,key);
        }else{
            return Result.error(CodeMsg.ACCESS_LIMIT_REACHED);
        }
        boolean check=miaoshaService.checkVerifyCode(user,goodsId,verifyCode);
        if(!check){
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }

        String path=miaoshaService.createMiaoshaPath(user,goodsId);

        return Result.success(path);
    }
    @RequestMapping(value = "/verifyCode", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaVerifyCode(HttpServletResponse response,Model model, MiaoshaUser user,
                                               @RequestParam("goodsId") long goodsId) {
        model.addAttribute("user", user);
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        try {
            BufferedImage image=miaoshaService.createVerifyCode(user,goodsId);
            OutputStream out=response.getOutputStream();
            ImageIO.write(image,"JPEG",out);
            out.flush();
            out.close();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(CodeMsg.MIAOSHA_FAIL);
        }
    }
}
