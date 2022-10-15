package com.baomidou.samples.shardingsphere.jdbc.v5.spring.service;

import com.baomidou.samples.shardingsphere.jdbc.v5.spring.entity.OrderPojo;

import java.util.List;

/**
 * @Author hl
 * @DateTime 2022/10/14 15:56
 */
public interface OrderPojoService {
    /**
     *
     * @return
     */
    List<OrderPojo> orderPojoList();

    List<OrderPojo> orderPojoListSlave();

    List<OrderPojo> insertOrderPojo();
}
