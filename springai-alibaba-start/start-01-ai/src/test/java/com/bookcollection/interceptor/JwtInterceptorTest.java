package com.bookcollection.interceptor;

import com.bookcollection.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class JwtInterceptorTest {

    private final JwtInterceptor interceptor = new JwtInterceptor();

    @Test
    void preHandle_shouldReturnTrueForValidBearerToken() {
        String token = JwtUtils.generateToken(1L, "alice");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_shouldReturnFalseWhenMissingHeaderOrInvalid() {
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request1, response1, new Object()));

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.addHeader("Authorization", "Bearer not-a-jwt");
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request2, response2, new Object()));

        MockHttpServletRequest request3 = new MockHttpServletRequest();
        request3.addHeader("Authorization", "token " + JwtUtils.generateToken(1L, "alice"));
        MockHttpServletResponse response3 = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request3, response3, new Object()));
    }
}
