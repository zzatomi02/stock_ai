package com.noono0.stock.common.web;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.execution.OrderGatewayException;
import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> dataAccess(DataAccessException ex) {
        Throwable root = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause() : ex;
        String msg = root.getMessage() != null ? root.getMessage() : ex.getMessage();
        log.error("Database error: {}", msg, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, null, "데이터 처리 중 오류가 발생했습니다."));
    }

    /** JPA/Hibernate (컬럼 불일치 등) — Spring 이 DataAccessException 으로 안 감싸는 경우 */
    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<ApiResponse<Void>> persistence(PersistenceException ex) {
        Throwable root = ex.getCause() != null ? ex.getCause() : ex;
        String msg = root.getMessage() != null ? root.getMessage() : ex.getMessage();
        log.error("JPA error: {}", msg, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, null, "데이터 처리 중 오류가 발생했습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> any(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, null, "서버 내부 오류가 발생했습니다."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> illegalState(IllegalStateException ex) {
        log.warn("Bad state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, null, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> illegalArg(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, null, ex.getMessage()));
    }

    @ExceptionHandler(OrderGatewayException.class)
    public ResponseEntity<ApiResponse<Void>> orderGateway(OrderGatewayException ex) {
        log.warn("Order gateway: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(false, null, ex.getMessage()));
    }
}
