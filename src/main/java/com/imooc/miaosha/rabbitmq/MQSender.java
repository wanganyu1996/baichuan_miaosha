package com.imooc.miaosha.rabbitmq;

import com.imooc.miaosha.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MQSender {
    private static Logger log=LoggerFactory.getLogger(MQSender.class);
    @Autowired
    AmqpTemplate amqpTemplatel;

    public void send(Object message){
        String msg=RedisService.beanToString(message);
        log.info("send message:"+message);
       amqpTemplatel.convertAndSend(MQConfig.QUEUE,msg);
    }
}
