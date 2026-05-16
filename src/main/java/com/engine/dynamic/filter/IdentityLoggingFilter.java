package com.engine.dynamic.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;

@Component
public class IdentityLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(IdentityLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            Enumeration<String> headerNames = httpRequest.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    if (headerName.toLowerCase().startsWith("x-user-id") ||
                        headerName.toLowerCase().startsWith("authorization") ||
                        headerName.toLowerCase().startsWith("x-tenant-id")) {
                        logger.info("Identity Header found: {} = {}", headerName, httpRequest.getHeader(headerName));
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }
}
