package com.noono0.stock.news.mapper;

import com.noono0.stock.news.domain.NewsArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NewsArticleMapper {

    int countByDedupHash(@Param("dedupHash") String dedupHash);

    int insert(NewsArticle row);

    List<NewsArticle> findByStockCode(@Param("stockCode") String stockCode, @Param("limit") int limit);
}
