package com.easyops.hospitalcard.config;

import com.easyops.hospitalcard.domain.product.CardProduct;
import com.easyops.hospitalcard.domain.product.CardProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.OffsetDateTime;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class StaffIdentityCardProductBootstrap {

    private static final String STAFF_IDENTITY_CODE = "STAFF_IDENTITY";

    private final CardProductRepository cardProductRepository;

    @Bean
    CommandLineRunner ensureStaffIdentityProduct() {
        return args -> {
            if (cardProductRepository.findByCode(STAFF_IDENTITY_CODE).isPresent()) {
                return;
            }
            CardProduct product = new CardProduct();
            product.setId(UUID.randomUUID());
            product.setCode(STAFF_IDENTITY_CODE);
            product.setName("Staff Identity Card");
            product.setDescription("Identity-only card for employee and staff verification");
            product.setMediumType("PHYSICAL");
            product.setUsageDomains("ID");
            product.setStatus("ACTIVE");
            OffsetDateTime now = OffsetDateTime.now();
            product.setCreatedAt(now);
            product.setUpdatedAt(now);
            cardProductRepository.save(product);
        };
    }
}
