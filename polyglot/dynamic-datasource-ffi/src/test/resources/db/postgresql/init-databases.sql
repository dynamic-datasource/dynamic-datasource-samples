--
-- Copyright © ${project.inceptionYear} organization baomidou
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- PostgreSQL 变体里每个数据源各用一个数据库，ds_slave_3 留给 POST /datasources 的动态添加演示。
-- compose.yaml 把本文件挂载到 /docker-entrypoint-initdb.d/，CI 与 JVM 单测也执行同一份脚本。
CREATE DATABASE ds_master;
CREATE DATABASE ds_slave_1;
CREATE DATABASE ds_slave_2;
CREATE DATABASE ds_slave_3;
CREATE DATABASE ds_order;
CREATE DATABASE ds_product;
CREATE DATABASE ds_account;
