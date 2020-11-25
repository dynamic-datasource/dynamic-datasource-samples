package com.baomidou.samples.multitenant.service.tenant;

import com.baomidou.samples.multitenant.entity.MultipleTenant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 多租户模式下租户信息表(MultipleTenant)表服务接口
 *
 * @author Vic Xu
 * @since 2020-11-24 15:05:15
 */
@Service
public interface MultipleTenantService extends IService<MultipleTenant>{

}