package com.noono0.stock.strategy.mapper;

import com.noono0.stock.strategy.domain.Strategy;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StrategyMapper {

    List<Strategy> selectAll();
}
