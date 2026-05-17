package com.noono0.stock.ops.trading;

import com.noono0.stock.ops.system.domain.SystemControl;
import com.noono0.stock.ops.system.mapper.SystemControlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TradingControlService {
    private final SystemControlMapper systemControlMapper;

    @Transactional(readOnly = true)
    public boolean isEmergencyStop() {
        SystemControl row = systemControlMapper.findById(1);
        return row != null && Boolean.TRUE.equals(row.getEmergencyStop());
    }

    @Transactional
    public void setEmergencyStop(boolean on) {
        SystemControl row = systemControlMapper.findById(1);
        LocalDateTime now = LocalDateTime.now();
        if (row == null) {
            SystemControl c = new SystemControl();
            c.setId(1);
            c.setEmergencyStop(on);
            c.setUpdatedAt(now);
            systemControlMapper.insert(c);
        } else {
            row.setEmergencyStop(on);
            row.setUpdatedAt(now);
            systemControlMapper.update(row);
        }
    }
}
