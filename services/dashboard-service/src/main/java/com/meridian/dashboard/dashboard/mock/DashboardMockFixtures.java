package com.meridian.dashboard.dashboard.mock;

import java.math.BigDecimal;
import java.util.List;

import com.meridian.dashboard.dashboard.dto.DolarResponse;
import com.meridian.dashboard.dashboard.dto.FxRateResponse;

/** Datos de demostración estáticos; no representan cotizaciones actuales. */
public final class DashboardMockFixtures {

    private DashboardMockFixtures() {
    }

    /** @return cotizaciones ficticias de dólar oficial y blue en ARS. */
    public static List<DolarResponse> getDolar() {
        return List.of(
                new DolarResponse("oficial", new BigDecimal("1000.00"), new BigDecimal("1040.00")),
                new DolarResponse("blue", new BigDecimal("1200.00"), new BigDecimal("1220.00")));
    }

    /** @return tipos de cambio ficticios desde USD hacia EUR y BRL. */
    public static List<FxRateResponse> getFx() {
        return List.of(
                new FxRateResponse("USD", "EUR", new BigDecimal("0.92")),
                new FxRateResponse("USD", "BRL", new BigDecimal("5.10")));
    }
}
