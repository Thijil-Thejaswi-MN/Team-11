package com.team11.insurance.underwriting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Verifies profile restrictions and fail-closed safety for {@link StubUnderwritingEngine}.
 * <p>
 * Validates that the development/test-only always-approve stub is registered only under
 * {@code dev} and {@code test} profiles, and is strictly excluded from {@code prod}.
 * Also verifies that dependent components in production fail closed if no real underwriting
 * engine bean is present.
 */
class UnderwritingProfileTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StubUnderwritingEngine.class);

    @Test
    void stubIsAvailableInDevProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(StubUnderwritingEngine.class);
                    assertThat(context).hasSingleBean(IUnderwriting.class);
                    IUnderwriting engine = context.getBean(IUnderwriting.class);
                    UnderwritingResult result = engine.evaluate(null);
                    assertThat(result.isApproved()).isTrue();
                    assertThat(result.getReason()).isEqualTo(StubUnderwritingEngine.STUB_REASON);
                });
    }

    @Test
    void stubIsAvailableInTestProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(StubUnderwritingEngine.class);
                    assertThat(context).hasSingleBean(IUnderwriting.class);
                });
    }

    @Test
    void stubCannotBeRegisteredInProductionProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(StubUnderwritingEngine.class);
                    assertThat(context).doesNotHaveBean(IUnderwriting.class);
                });
    }

    @Test
    void productionFailsSafelyWhenUnderwritingEngineMissing() {
        // A consumer component that requires IUnderwriting must fail startup in prod
        // rather than silently falling back to the always-approve stub.
        new ApplicationContextRunner()
                .withUserConfiguration(StubUnderwritingEngine.class, UnderwritingConsumer.class)
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(org.springframework.beans.factory.NoSuchBeanDefinitionException.class)
                            .hasMessageContaining("com.team11.insurance.underwriting.IUnderwriting");
                });
    }

    @Configuration
    static class UnderwritingConsumer {
        private final IUnderwriting underwriting;

        @Autowired
        public UnderwritingConsumer(IUnderwriting underwriting) {
            this.underwriting = underwriting;
        }

        public IUnderwriting getUnderwriting() {
            return underwriting;
        }
    }
}
