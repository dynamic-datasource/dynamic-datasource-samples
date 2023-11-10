/*
 * Copyright © ${project.inceptionYear} organization baomidou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.samples.shardingsphere.jdbc.v4.spring.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.samples.shardingsphere.jdbc.v4.spring.entity.TOrder;
import com.baomidou.samples.shardingsphere.jdbc.v4.spring.mapper.TOrderMapper;
import com.baomidou.samples.shardingsphere.jdbc.v4.spring.service.TOrderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

/**
 * TODO Unit tests for ShardingSphere 5.4.0 need to be synchronized
 */
@Service
public class TOrderServiceImpl implements TOrderService {
    private final TOrderMapper tOrderMapper;

    public TOrderServiceImpl(TOrderMapper tOrderMapper) {
        this.tOrderMapper = tOrderMapper;
    }

    @Override
    @DS("shardingDataSourceInShardingSphere")
    public List<TOrder> findAll() {
        return tOrderMapper.findAll();
    }

    @Override
    @DS("shardingDataSourceInShardingSphere")
    public List<TOrder> addAll() {
        IntStream.range(0, 5)
                .forEach(i -> tOrderMapper.addAll(i + 114514L, "测试" + i, (long) i));
        return tOrderMapper.findAll();
    }
}
