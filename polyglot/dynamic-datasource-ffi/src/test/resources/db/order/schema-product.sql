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

DROP TABLE IF EXISTS product;
CREATE TABLE product
(
    id    BIGINT PRIMARY KEY,
    price DOUBLE PRECISION NOT NULL,
    stock INT              NOT NULL,
    -- 库存不足时让扣减语句在数据库层失败，批量事务（POST /orders/batch）据此整体回滚
    CONSTRAINT ck_product_stock CHECK (stock >= 0)
);
