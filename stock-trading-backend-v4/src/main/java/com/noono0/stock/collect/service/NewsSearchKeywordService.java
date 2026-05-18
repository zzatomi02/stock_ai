package com.noono0.stock.collect.service;

import com.noono0.stock.collect.domain.NewsSearchKeyword;
import com.noono0.stock.collect.dto.NewsSearchKeywordRequest;
import com.noono0.stock.collect.repository.NewsSearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsSearchKeywordService {
    private final NewsSearchKeywordRepository repository;

    public List<NewsSearchKeyword> list(Boolean enabled, String keywordGroup, String searchType) {
        List<NewsSearchKeyword> all = repository.findAllByOrderByPriorityAscIdAsc();
        return all.stream()
                .filter(k -> enabled == null || enabled.equals(k.getEnabled()))
                .filter(k -> !StringUtils.hasText(keywordGroup) || keywordGroup.equalsIgnoreCase(k.getKeywordGroup()))
                .filter(k -> !StringUtils.hasText(searchType) || searchType.equalsIgnoreCase(k.getSearchType()))
                .toList();
    }

    public NewsSearchKeyword get(long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("검색 키워드 없음: " + id));
    }

    @Transactional
    public NewsSearchKeyword create(NewsSearchKeywordRequest req) {
        NewsSearchKeyword k = new NewsSearchKeyword();
        apply(k, req);
        if (k.getEnabled() == null) {
            k.setEnabled(true);
        }
        return repository.save(k);
    }

    @Transactional
    public NewsSearchKeyword update(long id, NewsSearchKeywordRequest req) {
        NewsSearchKeyword k = get(id);
        apply(k, req);
        return repository.save(k);
    }

    @Transactional
    public NewsSearchKeyword setEnabled(long id, boolean enabled) {
        NewsSearchKeyword k = get(id);
        k.setEnabled(enabled);
        return repository.save(k);
    }

    @Transactional
    public void delete(long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("검색 키워드 없음: " + id);
        }
        repository.deleteById(id);
    }

    public List<NewsSearchKeyword> findDueKeywords() {
        LocalDateTime now = LocalDateTime.now();
        return repository.findByEnabledTrueOrderByPriorityAscIdAsc().stream()
                .filter(k -> isDue(k, now))
                .toList();
    }

    private static boolean isDue(NewsSearchKeyword k, LocalDateTime now) {
        if (k.getLastCollectedAt() == null) {
            return true;
        }
        int interval = k.getIntervalSeconds() != null ? k.getIntervalSeconds() : 300;
        return !k.getLastCollectedAt().plusSeconds(interval).isAfter(now);
    }

    private static void apply(NewsSearchKeyword k, NewsSearchKeywordRequest req) {
        k.setKeyword(req.keyword().trim());
        k.setKeywordGroup(req.keywordGroup().trim().toUpperCase());
        k.setSearchType(req.searchType().trim().toUpperCase());
        k.setPriority(req.priority());
        k.setIntervalSeconds(req.intervalSeconds());
        k.setDisplayCount(req.displayCount());
        k.setSortType(StringUtils.hasText(req.sortType()) ? req.sortType() : "date");
        if (req.enabled() != null) {
            k.setEnabled(req.enabled());
        }
        k.setDescription(req.description());
    }
}
