package com.meridian.dashboard.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meridian.dashboard.dashboard.dto.DashboardResponse;
import com.meridian.dashboard.dashboard.service.DashboardService;

import lombok.RequiredArgsConstructor;

/** Expone los datos agregados de proveedores públicos. */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** @return respuesta 200 con datos disponibles y secciones degraded ante fallos de proveedores. */
    @GetMapping
    public DashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }
}
