package com.meridian.dashboard.dashboard.mapper;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import org.mapstruct.Mapper;

import com.meridian.dashboard.dashboard.dto.CryptoPricePoint;
import com.meridian.dashboard.dashboard.dto.CryptoResponse;

/** Normaliza los valores numéricos del proveedor sin perder precisión decimal. */
@Mapper(componentModel = "spring")
public interface CryptoMapper {

    default CryptoResponse toPrice(String id, String symbol, JsonNode quote) {
        return new CryptoResponse(id, symbol, number(quote.required("usd")),
                number(quote.required("usd_24h_change")));
    }

    default CryptoPricePoint toPoint(JsonNode tuple) {
        if (!tuple.isArray() || tuple.size() != 2 || !tuple.get(0).canConvertToLong()) {
            throw new IllegalArgumentException("Punto histórico inválido");
        }
        return new CryptoPricePoint(Instant.ofEpochMilli(tuple.get(0).longValue()), number(tuple.get(1)));
    }

    private java.math.BigDecimal number(JsonNode value) {
        if (!value.isNumber()) {
            throw new IllegalArgumentException("Valor numérico requerido");
        }
        return value.decimalValue();
    }
}
