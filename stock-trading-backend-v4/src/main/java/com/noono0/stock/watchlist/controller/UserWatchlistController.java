package com.noono0.stock.watchlist.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.watchlist.domain.UserWatchlist;
import com.noono0.stock.watchlist.service.UserWatchlistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class UserWatchlistController {
    private final UserWatchlistService watchlistService;

    @GetMapping
    public ApiResponse<List<UserWatchlist>> list(HttpServletRequest req) {
        return ApiResponse.ok(watchlistService.list(req.getHeader("X-User-Id")));
    }

    @PostMapping
    public ApiResponse<UserWatchlist> add(
            HttpServletRequest req, @RequestBody Map<String, Object> body) {
        String stockCode = String.valueOf(body.getOrDefault("stockCode", ""));
        String stockName = body.get("stockName") != null ? String.valueOf(body.get("stockName")) : null;
        Integer sortOrder = body.get("sortOrder") instanceof Number n ? n.intValue() : null;
        return ApiResponse.ok(
                watchlistService.add(req.getHeader("X-User-Id"), stockCode, stockName, sortOrder));
    }

    @DeleteMapping("/{stockCode}")
    public ApiResponse<?> remove(HttpServletRequest req, @PathVariable("stockCode") String stockCode) {
        watchlistService.remove(req.getHeader("X-User-Id"), stockCode);
        return ApiResponse.ok(null, "삭제됨");
    }
}
