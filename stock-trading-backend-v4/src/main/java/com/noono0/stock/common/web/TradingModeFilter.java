package com.noono0.stock.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 프론트에서 {@code X-Trading-Mode: paper|real} 로 넘기면 요청 속성에 저장.
 * 이후 KIS 연동 시 {@link #ATTR_TRADING_MODE} 로 credential 분기에 사용.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TradingModeFilter extends OncePerRequestFilter {

    public static final String ATTR_TRADING_MODE = "app.tradingMode";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String h = request.getHeader("X-Trading-Mode");
        if (StringUtils.hasText(h)) {
            String m = h.trim().toLowerCase();
            if ("paper".equals(m) || "real".equals(m)) {
                request.setAttribute(ATTR_TRADING_MODE, m);
            }
        }
        filterChain.doFilter(request, response);
    }
}
