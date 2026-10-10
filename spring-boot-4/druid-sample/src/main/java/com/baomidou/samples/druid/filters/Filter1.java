package com.baomidou.samples.druid.filters;

import com.alibaba.druid.filter.FilterAdapter;
import com.alibaba.druid.filter.FilterChain;
import com.alibaba.druid.proxy.jdbc.PreparedStatementProxy;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("myFilter1")
public class Filter1 extends FilterAdapter {

    @Override
    public int preparedStatement_executeUpdate(FilterChain chain, PreparedStatementProxy statement)
            throws SQLException {
        log.info("ssss");
        return super.preparedStatement_executeUpdate(chain, statement);
    }
}
