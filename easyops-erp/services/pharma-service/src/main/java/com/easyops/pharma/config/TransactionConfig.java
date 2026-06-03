package com.easyops.pharma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionConfig {

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    /** For use when running logic in a new transaction (e.g. coverage calculation) so failures don't affect caller tx. */
    @Bean(name = "requiresNewTransactionTemplate")
    public TransactionTemplate requiresNewTransactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate tm = new TransactionTemplate(transactionManager);
        tm.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tm;
    }
}
