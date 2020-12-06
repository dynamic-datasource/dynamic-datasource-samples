package com.baomidou.samples.localtx.service;

import com.baomidou.samples.localtx.dto.PlaceOrderRequest;

public interface OrderService {

    /**
     * 下单
     *
     * @param placeOrderRequest 订单请求参数
     */
    void placeOrder(PlaceOrderRequest placeOrderRequest);
}