package com.team11.insurance.policies;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The one place that decides how money is rounded. SRS 4.6.2 and SAD 4.1.6 say every money amount is
 * rounded DOWN to the whole rupee. Change ROUNDING here (and the day-16 test) if the team ever decides otherwise.
 */
public final class Rupees {

    public static final RoundingMode ROUNDING = RoundingMode.DOWN;

    private Rupees() { }

    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(0, ROUNDING);
    }
}
