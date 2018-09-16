package com.imooc.miaosha.service;

import com.imooc.miaosha.domain.MiaoshaOrder;
import com.imooc.miaosha.redis.MiaoshaKey;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.util.MD5Util;
import com.imooc.miaosha.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.domain.OrderInfo;
import com.imooc.miaosha.vo.GoodsVo;

@Service
public class MiaoshaService {
	
	@Autowired
	GoodsService goodsService;
	
	@Autowired
	OrderService orderService;

	@Autowired
	RedisService redisService;

	@Transactional
	public OrderInfo miaosha(MiaoshaUser user, GoodsVo goods) {
		//减库存 下订单 写入秒杀订单
		boolean success=goodsService.reduceStock(goods);
		if(success){
		//order_info maiosha_order
		return orderService.createOrder(user, goods);
		}else{
			setGoodsOver(goods.getId());
			return null;
		}
	}



	public long getMiaoshaReult(Long userId, long goodsId) {
      MiaoshaOrder order=orderService.getMiaoshaOrderByUserIdGoodsId(userId,goodsId);
	  if(order!=null){//秒杀成功
	  	return order.getOrderId();
	  }else{
	  	boolean isOver=getGoodsOver(goodsId);
	  	if(isOver){
         return -1;
		}else{
	  		return 0;
		}
	  }
	}
	private void setGoodsOver(Long goodsId) {
		redisService.set(MiaoshaKey.isGoodsOver,""+goodsId,true);

	}
	private boolean getGoodsOver(long goodsId) {
		return redisService.exists(MiaoshaKey.isGoodsOver,""+goodsId);
	}

	public boolean checkPath(MiaoshaUser user, long goodsId, String path) {
		if(user==null||path==null){
			return false;
		}
		String pathOld=redisService.get(MiaoshaKey.getMiaoshaPath,""+user.getId()+"_"+goodsId,String.class);
		return path.equals(pathOld);
	}

	public String createMiaoshaPath(MiaoshaUser user, long goodsId) {
		String str=MD5Util.md5(UUIDUtil.uuid()+"123456");
		redisService.set(MiaoshaKey.getMiaoshaPath,""+user.getId()+"_"+goodsId,str);
		return str;
	}
}
