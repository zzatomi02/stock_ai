package com.noono0.stock.news.mapper;

import com.noono0.stock.news.domain.NewsKeywordRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NewsKeywordRuleMapper {

    List<NewsKeywordRule> findAll();

    List<NewsKeywordRule> findByActiveTrue();

    long count();

    int insert(NewsKeywordRule row);

    NewsKeywordRule findById(long id);

    int update(NewsKeywordRule row);

    int deleteById(long id);
}
