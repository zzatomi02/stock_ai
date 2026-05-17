package com.noono0.stock.watchlist.service;

import com.noono0.stock.watchlist.domain.UserWatchlist;
import com.noono0.stock.watchlist.repository.UserWatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserWatchlistService {
    private final UserWatchlistRepository repository;

    public List<UserWatchlist> list(String userId) {
        requireUser(userId);
        return repository.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId.trim());
    }

    @Transactional
    public UserWatchlist add(String userId, String stockCode, String stockName, Integer sortOrder) {
        requireUser(userId);
        if (!StringUtils.hasText(stockCode)) {
            throw new IllegalArgumentException("stockCode 필수");
        }
        String code = stockCode.trim();
        return repository
                .findByUserIdAndStockCode(userId.trim(), code)
                .orElseGet(
                        () -> {
                            UserWatchlist w = new UserWatchlist();
                            w.setUserId(userId.trim());
                            w.setStockCode(code);
                            w.setStockName(stockName);
                            w.setSortOrder(sortOrder != null ? sortOrder : 0);
                            return repository.save(w);
                        });
    }

    @Transactional
    public void remove(String userId, String stockCode) {
        requireUser(userId);
        repository.deleteByUserIdAndStockCode(userId.trim(), stockCode.trim());
    }

    private static void requireUser(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("X-User-Id 헤더 또는 로그인 user-id 필요");
        }
    }
}
