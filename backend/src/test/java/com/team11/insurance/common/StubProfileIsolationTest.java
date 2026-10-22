package com.team11.insurance.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.team11.insurance.claims.ClaimServiceStub;
import com.team11.insurance.claims.IClaimService;
import com.team11.insurance.notifications.INotify;
import com.team11.insurance.notifications.NotificationServiceStub;
import com.team11.insurance.payments.IPaymentService;
import com.team11.insurance.payments.PaymentServiceStub;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("P4-06 Stub Profile Isolation Security Tests")
class StubProfileIsolationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    PaymentServiceStub.class,
                    NotificationServiceStub.class,
                    ClaimServiceStub.class
            );

    @Test
    @DisplayName("Under 'prod' profile, all development stubs MUST NOT be registered")
    void testStubsNotLoadedUnderProdProfile() {
        contextRunner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PaymentServiceStub.class);
                    assertThat(context).doesNotHaveBean(NotificationServiceStub.class);
                    assertThat(context).doesNotHaveBean(ClaimServiceStub.class);
                    assertThat(context).doesNotHaveBean(IPaymentService.class);
                    assertThat(context).doesNotHaveBean(INotify.class);
                    assertThat(context).doesNotHaveBean(IClaimService.class);
                });
    }

    @Test
    @DisplayName("Under 'test' profile, development stubs are loaded as active beans")
    void testStubsLoadedUnderTestProfile() {
        contextRunner.withPropertyValues("spring.profiles.active=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(PaymentServiceStub.class);
                    assertThat(context).hasSingleBean(NotificationServiceStub.class);
                    assertThat(context).hasSingleBean(ClaimServiceStub.class);
                });
    }

    @Test
    @DisplayName("Under 'dev' profile, development stubs are loaded as active beans")
    void testStubsLoadedUnderDevProfile() {
        contextRunner.withPropertyValues("spring.profiles.active=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(PaymentServiceStub.class);
                    assertThat(context).hasSingleBean(NotificationServiceStub.class);
                    assertThat(context).hasSingleBean(ClaimServiceStub.class);
                });
    }
}
