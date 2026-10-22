package com.meridian.dashboard.dashboard.mapper;

import java.math.BigDecimal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.meridian.dashboard.dashboard.adapter.DolarQuote;
import com.meridian.dashboard.dashboard.dto.DolarResponse;
import com.meridian.dashboard.dashboard.dto.FxRateResponse;

/** Normaliza las cotizaciones externas sin exponer sus contratos al consumidor. */
@Mapper(componentModel = "spring")
public interface CurrencyMapper {

    @Mapping(target = "tipo", source = "casa")
    DolarResponse toDolarResponse(DolarQuote quote);

    FxRateResponse toFxRateResponse(String from, String to, BigDecimal rate);
}
