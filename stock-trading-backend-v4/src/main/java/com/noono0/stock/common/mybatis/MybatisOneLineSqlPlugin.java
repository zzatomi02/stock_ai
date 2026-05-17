package com.noono0.stock.common.mybatis;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * P6Spy의 {@code %(sqlSingleLine)}이 MySQL/드라이버에 따라 ?만 남는 경우를 보완해,
 * MyBatis {@link BoundSql}의 SQL 본문과 {@link BoundSql#getParameterObject()}를 한 줄에 남긴다.
 * 로컬: {@code logging.level.com.noono0.stock.mybatis.oneline: debug}
 */
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(
                type = Executor.class,
                method = "query",
                args = {
                    MappedStatement.class,
                    Object.class,
                    RowBounds.class,
                    ResultHandler.class,
                    org.apache.ibatis.cache.CacheKey.class,
                    org.apache.ibatis.mapping.BoundSql.class
                })
})
public class MybatisOneLineSqlPlugin implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger("com.noono0.stock.mybatis.oneline");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        if (args.length >= 2 && args[0] instanceof MappedStatement ms) {
            Object param = args[1];
            BoundSql boundSql;
            if (args.length >= 6 && args[5] instanceof BoundSql fromArg) {
                boundSql = fromArg;
            } else {
                boundSql = ms.getBoundSql(param);
            }
            if (boundSql != null && log.isDebugEnabled()) {
                String sql = boundSql.getSql();
                String oneLine = sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
                log.debug("SQL={} | params={}", oneLine, boundSql.getParameterObject());
            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // no-op
    }
}
