package com.meridian.dashboard.dashboard.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Catálogo público y símbolo interno de cada proveedor de series históricas. */
@Getter
@RequiredArgsConstructor
public enum SupportedAsset {
    BTC("bitcoin", true),
    ETH("ethereum", true),
    AAPL("AAPL", false),
    MSFT("MSFT", false),
    SPY("SPY", false);

    private final String symbol;
    private final boolean crypto;
}
