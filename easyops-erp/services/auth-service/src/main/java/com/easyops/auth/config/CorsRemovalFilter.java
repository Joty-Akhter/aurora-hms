package com.easyops.auth.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * CORS Removal Filter
 * 
 * Removes CORS headers from responses since API Gateway handles CORS.
 * This prevents duplicate CORS headers.
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
        
        // Wrap response to intercept header setting
        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(httpResponse) {
            @Override
            public void setHeader(String name, String value) {
                // Skip CORS headers - let API Gateway handle them
                if (!name.startsWith("Access-Control-")) {
                    super.setHeader(name, value);
                }
            }
            
            @Override
            public void addHeader(String name, String value) {
                // Skip CORS headers - let API Gateway handle them
                if (!name.startsWith("Access-Control-")) {
                    super.addHeader(name, value);
                }
            }
        };
        
        chain.doFilter(request, wrapper);
    }
}

