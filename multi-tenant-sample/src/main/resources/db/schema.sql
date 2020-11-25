CREATE TABLE `core_tenant`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '组织或个人名称',
  `tenant_name` varchar(40) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '租户名称(自动赋值且唯一)',
  `identity_information` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '租户身份识别码',
  `user_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '数据库用户名',
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '数据库密码',
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '数据库连接地址',
  `driver_name` varchar(30) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '数据库驱动',
  `gmt_create` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
  `gmt_modified` datetime(0) NULL DEFAULT NULL COMMENT '修改日期',
  PRIMARY KEY (`id`) USING BTREE
);

CREATE TABLE IF NOT EXISTS `USER`
(
    id     BIGINT(20) NOT NULL AUTO_INCREMENT,
    name   VARCHAR(30) NULL DEFAULT NULL,
    age    INT(11) NULL DEFAULT NULL,
    PRIMARY KEY (id)
);