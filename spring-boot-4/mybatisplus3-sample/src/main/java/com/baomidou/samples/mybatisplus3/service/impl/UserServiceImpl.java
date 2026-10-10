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
package com.baomidou.samples.mybatisplus3.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.Slave;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.repository.AbstractRepository;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.baomidou.samples.mybatisplus3.entity.User;
import com.baomidou.samples.mybatisplus3.mapper.UserMapper;
import com.baomidou.samples.mybatisplus3.service.UserService;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl extends AbstractRepository<UserMapper, User> implements UserService {

    @Autowired private UserMapper baseMapper;

    @Override
    public UserMapper getBaseMapper() {
        return baseMapper;
    }

    @Override
    public List<User> selectMasterUsers() {
        return baseMapper.selectList(null);
    }

    @Override
    @DS("slave")
    public List<User> selectSlaveUsers() {
        return baseMapper.selectList(null);
    }

    @Override
    public List<User> selectLambdaMasterUsers() {
        return this.lambdaQuery().list();
    }

    @Override
    @DS("slave")
    public List<User> selectLambdaSlaveUsers() {
        return this.lambdaQuery().list();
    }

    @Override
    @Slave
    public List<User> selectSlaveAnnotationUsers() {
        return this.lambdaQuery().list();
    }

    @Override
    public void addUser(User user) {
        baseMapper.insert(user);
    }

    @Override
    public void deleteUserById(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<User> entityList, int batchSize) {
        String sqlStatement = SqlHelper.getSqlStatement(getMapperClass(), SqlMethod.INSERT_ONE);
        return executeBatch(
                entityList,
                batchSize,
                (sqlSession, entity) -> sqlSession.insert(sqlStatement, entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBatch(Collection<User> entityList, int batchSize) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(User.class);
        Assert.notNull(
                tableInfo,
                "error: can not execute. because can not find cache of TableInfo for entity!");
        String keyProperty = tableInfo.getKeyProperty();
        Assert.notEmpty(
                keyProperty,
                "error: can not execute. because can not find column for id from entity!");
        String updateStatement =
                SqlHelper.getSqlStatement(getMapperClass(), SqlMethod.UPDATE_BY_ID);
        return SqlHelper.saveOrUpdateBatch(
                getSqlSessionFactory(),
                getMapperClass(),
                log,
                entityList,
                batchSize,
                (sqlSession, entity) ->
                        StringUtils.checkValNull(tableInfo.getPropertyValue(entity, keyProperty)),
                // BaseMapper.updateById 的参数声明为 @Param("et")，因此实体必须包一层再传入。
                (sqlSession, entity) ->
                        sqlSession.update(updateStatement, Collections.singletonMap("et", entity)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchById(Collection<User> entityList, int batchSize) {
        String sqlStatement = SqlHelper.getSqlStatement(getMapperClass(), SqlMethod.UPDATE_BY_ID);
        // BaseMapper.updateById 的参数声明为 @Param("et")，因此实体必须包一层再传入。
        return executeBatch(
                entityList,
                batchSize,
                (sqlSession, entity) ->
                        sqlSession.update(sqlStatement, Collections.singletonMap("et", entity)));
    }
}
