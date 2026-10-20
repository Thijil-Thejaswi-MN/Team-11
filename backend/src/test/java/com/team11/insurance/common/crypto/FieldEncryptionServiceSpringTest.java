package com.team11.insurance.common.crypto;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("P2-05 / INS-SR-004: Spring-Managed FieldEncryptionService Regression Tests")
class FieldEncryptionServiceSpringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FieldEncryptionService.class);

    private static final String TEST_PROD_KEY_32BYTES_UTF8 = "prod-test-key-32bytes-secure123!";
    private static final String TEST_PROD_KEY_BASE64 = Base64.getEncoder().encodeToString(TEST_PROD_KEY_32BYTES_UTF8.getBytes(StandardCharsets.UTF_8));

    @BeforeEach
    void setUp() {
        FieldEncryptionService.resetDefaultInstance();
    }

    @AfterEach
    void tearDown() {
        FieldEncryptionService.resetDefaultInstance();
    }

    @Test
    @DisplayName("Test A — Production with missing key: context initialization fails, dev key is never selected")
    void testA_productionWithMissingKey_failsClosed() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure).isNotNull();
                    assertThat(failure)
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .rootCause()
                            .hasMessageContaining("CRITICAL SECURITY VIOLATION")
                            .hasMessageContaining("MUST be configured in production profile");
                });
    }

    @Test
    @DisplayName("Test B1 — Production with key shorter than 32 bytes: context initialization fails clearly")
    void testB1_productionWithShortKey_failsInitialization() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "insurance.security.encryption-key=short-key"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure).isNotNull();
                    assertThat(failure)
                            .hasRootCauseInstanceOf(IllegalArgumentException.class)
                            .rootCause()
                            .hasMessageContaining("Encryption key must be exactly 32 bytes");
                });
    }

    @Test
    @DisplayName("Test B2 — Production with default dev key: context initialization fails closed")
    void testB2_productionWithDefaultDevKey_failsInitialization() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "insurance.security.encryption-key=" + FieldEncryptionService.DEFAULT_DEV_KEY
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure).isNotNull();
                    assertThat(failure)
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .rootCause()
                            .hasMessageContaining("CRITICAL SECURITY VIOLATION")
                            .hasMessageContaining("cannot use default dev key");
                });
    }

    @Test
    @DisplayName("Test B3 — Production with empty or whitespace key: context initialization fails closed")
    void testB3_productionWithEmptyKey_failsInitialization() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "insurance.security.encryption-key=   "
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure).isNotNull();
                    assertThat(failure)
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .rootCause()
                            .hasMessageContaining("CRITICAL SECURITY VIOLATION")
                            .hasMessageContaining("MUST be configured in production profile");
                });
    }

    @Test
    @DisplayName("Test C1 — Production with valid Base64 AES-256 key: service initializes and encrypts/decrypts")
    void testC1_productionWithValidBase64Key_initializesSuccessfully() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "insurance.security.encryption-key=" + TEST_PROD_KEY_BASE64
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FieldEncryptionService service = context.getBean(FieldEncryptionService.class);
                    assertThat(service).isNotNull();

                    String plaintext = "PROD_CONFIDENTIAL_DATA";
                    String encrypted = service.encrypt(plaintext);
                    assertThat(encrypted).startsWith(FieldEncryptionService.VERSION_PREFIX);
                    assertThat(service.decrypt(encrypted)).isEqualTo(plaintext);

                    // Ensure secret key material is never exposed in toString
                    assertThat(service.toString()).doesNotContain(TEST_PROD_KEY_32BYTES_UTF8);
                    assertThat(service.toString()).doesNotContain(TEST_PROD_KEY_BASE64);
                });
    }

    @Test
    @DisplayName("Test C2 — Production with valid 32-byte UTF-8 key: service initializes and operates securely")
    void testC2_productionWithValidUtf8Key_initializesSuccessfully() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=production",
                        "insurance.security.encryption-key=" + TEST_PROD_KEY_32BYTES_UTF8
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FieldEncryptionService service = context.getBean(FieldEncryptionService.class);
                    assertThat(service).isNotNull();

                    String plaintext = "PROD_CUSTOMER_AADHAAR_9999";
                    String encrypted = service.encrypt(plaintext);
                    assertThat(service.decrypt(encrypted)).isEqualTo(plaintext);
                });
    }

    @Test
    @DisplayName("Test D — Spring actually injects Environment via constructor: bean detects active production profile")
    void testD_springInjectsEnvironment_andDetectsProductionProfile() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "insurance.security.encryption-key=" + TEST_PROD_KEY_BASE64
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FieldEncryptionService service = context.getBean(FieldEncryptionService.class);

                    // Assert that Spring constructor injection reliably provided the real Environment
                    assertThat(service.getEnvironment()).isNotNull();
                    assertThat(service.isProduction()).isTrue();
                    assertThat(service.getEnvironment().getActiveProfiles()).contains("prod");
                });
    }

    @Test
    @DisplayName("Test E — Development fallback behaviour: permitted in dev/test, forbidden in production")
    void testE_devFallbackPermittedInDev_forbiddenInProd() {
        // 1. In dev profile with no key configured, fallback to dev key is allowed
        contextRunner
                .withPropertyValues("spring.profiles.active=dev")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FieldEncryptionService service = context.getBean(FieldEncryptionService.class);
                    assertThat(service.isProduction()).isFalse();

                    String sample = "DEV_TEST_VALUE";
                    String enc = service.encrypt(sample);
                    assertThat(service.decrypt(enc)).isEqualTo(sample);
                });

        // 2. In test profile with no key configured, fallback is allowed
        contextRunner
                .withPropertyValues("spring.profiles.active=test")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FieldEncryptionService service = context.getBean(FieldEncryptionService.class);
                    assertThat(service.isProduction()).isFalse();
                });

        // 3. In production profile with the identical missing-key setup, fallback is strictly rejected
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .rootCause()
                            .hasMessageContaining("CRITICAL SECURITY VIOLATION");
                });
    }
}
