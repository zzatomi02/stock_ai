package com.noono0.stock.news.mapper;

import com.noono0.stock.news.domain.StockSymbolMap;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StockSymbolMapMapper {

    List<StockSymbolMap> findAll();

    int upsert(StockSymbolMap row);
}
