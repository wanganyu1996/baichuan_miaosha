package com.imooc.miaosha.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class MQReciver {
    private static Logger log=LoggerFactory.getLogger(MQReciver.class);
    @RabbitListener(queues=MQConfig.QUEUE)
    public void receive(String message){
       log.info("receive message:"+message);
    }
}
