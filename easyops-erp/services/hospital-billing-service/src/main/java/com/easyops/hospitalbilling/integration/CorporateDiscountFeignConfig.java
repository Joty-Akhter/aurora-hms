package com.easyops.hospitalbilling.integration;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propagates {@code X-User-Id} / {@code X-Organization-Id} from the inbound billing request to
 * {@code hospital-corporate-and-discount-service} so RBAC on evaluation endpoints can be satisfied.
 */
@Configuration
public class CorporateDiscountFeignConfig {

    @Bean
    public RequestInterceptor propagateUserAndOrgHeaders() {
        return requestTemplate -> {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletRequestAttributes) {
                HttpServletRequest req = servletRequestAttributes.getRequest();
                String uid = req.getHeader("X-User-Id");
                if (uid != null && !uid.isBlank()) {
                    requestTemplate.header("X-User-Id", uid);
                }
                String org = req.getHeader("X-Organization-Id");
                if (org != null && !org.isBlank()) {
                    requestTemplate.header("X-Organization-Id", org);
                }
            }
        };
    }
}
