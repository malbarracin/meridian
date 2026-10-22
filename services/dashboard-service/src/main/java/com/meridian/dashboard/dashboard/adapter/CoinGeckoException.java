package com.meridian.dashboard.dashboard.adapter;

import lombok.Getter;

/** Fallo del proveedor que el agregador debe reflejar en degraded. */
@Getter
public class CoinGeckoException extends RuntimeException {

    private final String section;

    public CoinGeckoException(String section, Throwable cause) {
        super("CoinGecko no disponible", cause);
        this.section = section;
    }
}
