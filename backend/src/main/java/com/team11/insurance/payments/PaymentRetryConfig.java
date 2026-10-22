package com.team11.insurance.payments;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Spring configuration providing the appropriate {@link PaymentRetryDelayer}
 * depending on whether the application is running under the test profile or not.
 */
@Configuration
public class PaymentRetryConfig {

    @Bean
    @Profile("!test")
    public PaymentRetryDelayer productionPaymentRetryDelayer() {
        return PaymentRetryDelayer.REAL;
    }

    @Bean
    @Profile("test")
    public PaymentRetryDelayer testPaymentRetryDelayer() {
        return PaymentRetryDelayer.NO_OP;
    }
}
