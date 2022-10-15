package com.baomidou.samples.shardingsphere.jdbc.v5.spring.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.samples.shardingsphere.jdbc.v5.spring.config.SnowFlakeUtil;
import com.baomidou.samples.shardingsphere.jdbc.v5.spring.entity.OrderPojo;
import com.baomidou.samples.shardingsphere.jdbc.v5.spring.mapper.OrderPojoMapper;
import com.baomidou.samples.shardingsphere.jdbc.v5.spring.service.OrderPojoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author hl
 * @DateTime 2022/10/14 15:56
 */
@Service
public class OrderPojoServiceImpl implements OrderPojoService {

    @Autowired
    private OrderPojoMapper orderPojoMapper;

    /**
     * 默认主数据库
     * @return
     */
    @Override
    public List<OrderPojo> orderPojoList() {
        return orderPojoMapper.orderPojoList();
    }

    @DS("shardingsphere")
    @Override
    public List<OrderPojo> orderPojoListSlave() {
        return orderPojoMapper.orderPojoList();
    }

    @Override
    public List<OrderPojo> insertOrderPojo() {
        SnowFlakeUtil snowFlakeUtil = new SnowFlakeUtil(2, 3);
        for (int i = 0; i < 5; i++) {
            OrderPojo orderPojo = new OrderPojo();
            orderPojo.setOrderId(snowFlakeUtil.createId())
                    .setName("测试"+i)
                    .setUserId((long) i);
            orderPojoMapper.insertOrderPojo(orderPojo);
        }
        return orderPojoMapper.orderPojoList();
    }
}
