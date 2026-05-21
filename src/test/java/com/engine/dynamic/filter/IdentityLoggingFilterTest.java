package com.engine.dynamic.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityLoggingFilterTest {

    private IdentityLoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new IdentityLoggingFilter();
    }

    @Test
    void testDoFilter() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "test-user");
        request.addHeader("Authorization", "Bearer token");
        request.addHeader("X-Tenant-Id", "test-tenant");
        request.addHeader("Other-Header", "other-value");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // This is a simple test to ensure no exceptions are thrown and the chain continues
        assertThat(chain.getRequest()).isEqualTo(request);
        assertThat(chain.getResponse()).isEqualTo(response);
    }
}
