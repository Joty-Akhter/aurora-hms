package com.easyops.users.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * CORS Removal Filter
 * 
 * Removes CORS headers from responses since API Gateway handles CORS.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsRemovalFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Wrap response to prevent CORS headers
        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(httpResponse) {
            @Override
            public void setHeader(String name, String value) {
                if (!name.startsWith("Access-Control-")) {
                    super.setHeader(name, value);
                }
            }
            
            @Override
            public void addHeader(String name, String value) {
                if (!name.startsWith("Access-Control-")) {
                    super.addHeader(name, value);
                }
            }
        };
        
        chain.doFilter(request, wrapper);
    }
}

