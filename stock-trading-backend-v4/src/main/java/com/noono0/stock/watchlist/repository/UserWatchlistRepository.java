package com.noono0.stock.watchlist.repository;

import com.noono0.stock.watchlist.domain.UserWatchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserWatchlistRepository extends JpaRepository<UserWatchlist, Long> {
    List<UserWatchlist> findByUserIdOrderBySortOrderAscCreatedAtAsc(String userId);

    Optional<UserWatchlist> findByUserIdAndStockCode(String userId, String stockCode);

    void deleteByUserIdAndStockCode(String userId, String stockCode);
}
