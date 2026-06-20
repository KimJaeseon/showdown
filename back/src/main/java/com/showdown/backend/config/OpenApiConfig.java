package com.showdown.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI showdownOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Showdown Tournament REST API")
                        .version("v1")
                        .description("Spring Boot, JPA, PostgreSQL 기반 쇼다운 대회 운영 API"))
                .schemaRequirement("basicAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic"))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}
