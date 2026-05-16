package com.bookcollection.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void success_withData_shouldSetCodeMessageAndData() {
        Result<String> result = Result.success("ok");

        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("ok", result.getData());
    }

    @Test
    void success_withoutData_shouldSetNullData() {
        Result<Object> result = Result.success();

        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void error_shouldSetCodeAndMessage() {
        Result<Object> result = Result.error("boom");

        assertEquals(500, result.getCode());
        assertEquals("boom", result.getMessage());
        assertNull(result.getData());
    }
}
