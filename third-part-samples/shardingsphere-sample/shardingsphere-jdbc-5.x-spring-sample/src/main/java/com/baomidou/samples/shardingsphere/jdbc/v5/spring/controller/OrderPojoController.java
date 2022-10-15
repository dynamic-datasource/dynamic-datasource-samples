package com.baomidou.samples.shardingsphere.jdbc.v5.spring.controller;

import com.baomidou.samples.shardingsphere.jdbc.v5.spring.entity.OrderPojo;
import com.baomidou.samples.shardingsphere.jdbc.v5.spring.service.OrderPojoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author hl
 * @DateTime 2022/10/14 15:55
 */
@RestController
@RequestMapping("/order/")
public class OrderPojoController {

    @Autowired
    private OrderPojoService orderPojoService;

    /**
     * master
     * @return
     */
    @GetMapping("orderPojoList")
    public List<OrderPojo> orderPojoList(){
       return orderPojoService.orderPojoList();
    }

    /**
     * master
     * @return
     */
    @PostMapping("insertOrderPojo")
    public List<OrderPojo> insertOrderPojo(){
       return orderPojoService.insertOrderPojo();
    }

    /**
     * slave
     * @return
     */
    @GetMapping("orderPojoListSlave")
    public List<OrderPojo> orderPojoListSlave(){
       return orderPojoService.orderPojoListSlave();
    }


}
