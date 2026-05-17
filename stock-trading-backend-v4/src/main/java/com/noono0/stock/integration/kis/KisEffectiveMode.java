package com.noono0.stock.integration.kis;

import com.noono0.stock.common.web.TradingModeFilter;
import com.noono0.stock.integration.kis.config.KisProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/** HTTP 요청의 X-Trading-Mode 우선, 없으면 app.kis.mode */
public final class KisEffectiveMode {
    private KisEffectiveMode() {}

    public static String from(KisProperties props, HttpServletRequest req) {
        if (req != null) {
            Object a = req.getAttribute(TradingModeFilter.ATTR_TRADING_MODE);
            if (a instanceof String s && StringUtils.hasText(s)) {
                return "real".equalsIgnoreCase(s.trim()) ? "real" : "paper";
            }
        }
        return "real".equalsIgnoreCase(props.getMode()) ? "real" : "paper";
    }

    public static String fromConfigOnly(KisProperties props) {
        return "real".equalsIgnoreCase(props.getMode()) ? "real" : "paper";
    }
}
