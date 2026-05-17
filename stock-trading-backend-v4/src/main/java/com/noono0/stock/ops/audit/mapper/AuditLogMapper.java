package com.noono0.stock.ops.audit.mapper;

import com.noono0.stock.ops.audit.domain.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper {

    int insert(AuditLog row);

    long count();
}
