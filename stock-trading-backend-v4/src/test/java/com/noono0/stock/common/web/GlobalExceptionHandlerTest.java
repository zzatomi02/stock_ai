package com.noono0.stock.common.web;

import com.noono0.stock.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void dataAccessExceptionDoesNotExposeInternalMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.dataAccess(
                new DataAccessResourceFailureException("jdbc:mysql://internal:3306 failed"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("데이터 처리 중 오류가 발생했습니다.");
    }

    @Test
    void genericExceptionDoesNotExposeInternalMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.any(
                new IllegalStateException("secret stack detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("서버 내부 오류가 발생했습니다.");
    }

    @Test
    void badRequestExceptionsKeepClientMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.illegalArg(
                new IllegalArgumentException("type must be volume or amount"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("type must be volume or amount");
    }
}
