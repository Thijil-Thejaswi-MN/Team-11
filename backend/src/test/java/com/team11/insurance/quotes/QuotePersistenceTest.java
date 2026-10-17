package com.team11.insurance.quotes;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Repository and persistence test for {@link Quote} entity (P1-05).
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuotePersistenceTest {

    @Autowired
    private QuoteRepository quotes;

    @Test
    void canPersistMotorQuoteWithBreakdownAndDetails() {
        Quote quote = new Quote(
                101L,
                1L,
                11L,
                "FOUR_WHEELER",
                new BigDecimal("9480.00"),
                "{\"basePremium\":8000,\"addons\":1480}",
                "{\"make\":\"Maruti\",\"model\":\"Swift\",\"regYear\":2019,\"idv\":400000}",
                Instant.now().plus(30, ChronoUnit.DAYS)
        );

        Quote saved = quotes.saveAndFlush(quote);

        assertNotNull(saved.getId());
        assertEquals(101L, saved.getCustomerId());
        assertEquals(1L, saved.getInsurerId());
        assertEquals(11L, saved.getPlanId());
        assertEquals("FOUR_WHEELER", saved.getProductType());
        assertEquals(new BigDecimal("9480.00"), saved.getPremium());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertNotNull(saved.getValidUntil());
    }

    @Test
    void canPersistHealthQuote() {
        Quote quote = new Quote(
                102L,
                2L,
                21L,
                "HEALTH",
                new BigDecimal("6000.00"),
                "{\"basePremium\":6000}",
                "{\"members\":[{\"age\":30},{\"age\":32}],\"sumInsured\":500000,\"preExisting\":false}",
                Instant.now().plus(15, ChronoUnit.DAYS)
        );

        Quote saved = quotes.saveAndFlush(quote);

        assertNotNull(saved.getId());
        assertEquals("HEALTH", saved.getProductType());
        assertEquals(new BigDecimal("6000.00"), saved.getPremium());
    }

    @Test
    void requiresMandatoryFields() {
        Quote invalid = new Quote();
        // Missing customerId, planId, productType, premium
        assertThrows(DataIntegrityViolationException.class, () -> quotes.saveAndFlush(invalid));
    }

    @Test
    void findByCustomerIdReturnsQuotesForCustomer() {
        Quote q1 = new Quote(200L, 1L, 11L, "TWO_WHEELER", new BigDecimal("1200.00"), null, null, null);
        Quote q2 = new Quote(200L, 1L, 12L, "FOUR_WHEELER", new BigDecimal("8500.00"), null, null, null);
        Quote q3 = new Quote(201L, 1L, 11L, "TWO_WHEELER", new BigDecimal("1200.00"), null, null, null);

        quotes.save(q1);
        quotes.save(q2);
        quotes.save(q3);
        quotes.flush();

        List<Quote> customerQuotes = quotes.findByCustomerId(200L);
        assertEquals(2, customerQuotes.size());

        List<Quote> motor2W = quotes.findByCustomerIdAndProductType(200L, "TWO_WHEELER");
        assertEquals(1, motor2W.size());
        assertEquals(new BigDecimal("1200.00"), motor2W.get(0).getPremium());

        List<Quote> plan11Quotes = quotes.findByPlanId(11L);
        assertEquals(2, plan11Quotes.size());
    }
}
