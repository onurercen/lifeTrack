package com.lifetrack.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "LifeTrack API",
        version = "1.0.0",
        description = "LifeTrack uygulaması için API dokümantasyonu"
    )
)
public class OpenApiConfig {
}
