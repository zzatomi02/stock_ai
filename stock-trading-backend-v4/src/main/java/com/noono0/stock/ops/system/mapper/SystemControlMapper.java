package com.noono0.stock.ops.system.mapper;

import com.noono0.stock.ops.system.domain.SystemControl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SystemControlMapper {

    SystemControl findById(@Param("id") int id);

    int insert(SystemControl row);

    int update(SystemControl row);
}
