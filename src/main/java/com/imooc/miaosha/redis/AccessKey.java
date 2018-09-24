package com.imooc.miaosha.redis;

public class AccessKey extends BasePrefix {
    private AccessKey(int expireSeconds, String prefix) {
        super(expireSeconds,prefix);
    }
    public static AccessKey access= new AccessKey(0,"access");

}
