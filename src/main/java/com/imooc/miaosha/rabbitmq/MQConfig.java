package com.imooc.miaosha.rabbitmq;


import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class MQConfig {

    public static final String MIAOSHA_QUEUE="miaosha_queue";
    public static final String QUEUE="queue";
    public static final String TOPIC_QUEUE1="topic.queue1";
    public static final String TOPIC_QUEUE2="topic.queue2";
    public static final String HEADER_QUEUE="header.queue";
    public static final String TOPIC_EXCHANGE="topicExchange";
    public static final String ROUTING_KEY1="topic.key1";
    public static final String ROUTING_KEY2="topic.#";
    public static final String FANOUT_EXCHANGE="fanoutExchange";
    public static final String HEADERS_EXCHANGE="headersExchange";
    /**
     * Direct模式
     * @return
     */
  @Bean
  public Queue queue(){
    return new Queue(MIAOSHA_QUEUE,true);
  }
    /**
     * Topic模式 交换机Exchange
     * @return
     */
    @Bean
    public Queue topQueue1(){
        return new Queue(TOPIC_QUEUE1,true);
    }
    @Bean
    public Queue topQueue2(){
        return new Queue(TOPIC_QUEUE2,true);
    }

    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(TOPIC_EXCHANGE);
    }
    @Bean
    public Binding topicBinding1(){
        return BindingBuilder.bind(topQueue1()).to(topicExchange()).with("topic.key1");
    }
    @Bean
    public Binding topicBinding2(){
        return BindingBuilder.bind(topQueue2()).to(topicExchange()).with("topic.#");
    }
    /**
     * Fanout模式 交换机Exchange
     */
    @Bean
    public FanoutExchange fanoutExchange(){
        return new FanoutExchange(FANOUT_EXCHANGE);
    }
    @Bean
    public Binding FanoutBinding1(){
        return BindingBuilder.bind(topQueue1()).to(fanoutExchange());
    }
    @Bean
    public Binding FanoutBinding2(){
        return BindingBuilder.bind(topQueue2()).to(fanoutExchange());
    }
    /**
     * Header模式 交换机Exchange
     */
    @Bean
    public HeadersExchange headersExchange(){
        return new HeadersExchange(HEADERS_EXCHANGE);
    }
    @Bean
    public Queue headerQueue1(){
        return new Queue(HEADER_QUEUE,true);
    }
    @Bean
    public Binding headerBinding(){
        Map<String,Object> map=new HashMap<String, Object>();
        map.put("header1","value1");
        map.put("header2","value2");
        return BindingBuilder.bind(headerQueue1()).to(headersExchange()).whereAll(map).match();
    }
}
