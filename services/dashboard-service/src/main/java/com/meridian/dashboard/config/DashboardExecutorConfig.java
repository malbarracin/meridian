package com.meridian.dashboard.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ejecuta las consultas bloqueantes de proveedores en hilos virtuales independientes. */
@Configuration
public class DashboardExecutorConfig {

    @Bean(destroyMethod = "close")
    ExecutorService dashboardExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
