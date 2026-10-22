package com.meridian.dashboard.dashboard.dto;

import java.util.List;

/** Serie OHLC inmutable de un ticker. */
public record CandleResponse(String ticker, List<OhlcPoint> ohlcPoints) {

    public CandleResponse {
        ohlcPoints = List.copyOf(ohlcPoints);
    }
}
