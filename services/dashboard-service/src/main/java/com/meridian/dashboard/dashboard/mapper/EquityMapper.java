package com.meridian.dashboard.dashboard.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.meridian.dashboard.dashboard.adapter.FinnhubQuote;
import com.meridian.dashboard.dashboard.dto.CandleResponse;
import com.meridian.dashboard.dashboard.dto.EquityQuoteResponse;
import com.meridian.dashboard.dashboard.dto.OhlcPoint;

/** Normaliza los contratos de bolsa del proveedor. */
@Mapper(componentModel = "spring")
public interface EquityMapper {

    @Mapping(target = "price", source = "quote.c")
    @Mapping(target = "currency", constant = "USD")
    EquityQuoteResponse toQuote(String ticker, FinnhubQuote quote);

    OhlcPoint toPoint(long t, BigDecimal o, BigDecimal h, BigDecimal l, BigDecimal c, BigDecimal v);

    CandleResponse toCandles(String ticker, List<OhlcPoint> ohlcPoints);
}
