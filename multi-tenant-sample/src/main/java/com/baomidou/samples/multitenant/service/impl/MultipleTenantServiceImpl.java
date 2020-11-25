package com.baomidou.samples.multitenant.service.tenant.impl;

import com.baomidou.samples.multitenant.dao.MutliptleTenantDao;
import com.baomidou.samples.multitenant.entity.MultipleTenant;
import com.baomidou.samples.multitenant.service.tenant.MultipleTenantService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 多租户模式下租户信息表(MultipleTenant)表服务实现类
 *
 * @author Vic Xu
 * @since 2020-11-24 15:05:15
 */
@Service
public class MultipleTenantServiceImpl extends ServiceImpl<MutliptleTenantDao,MultipleTenant> implements MultipleTenantService {

}