package com.rbac.iam.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        final String basicSchemeName = "basicAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("RBAC API Documentation")
                        .version("1.0.0")
                        .description("API document for RBAC System"))

                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        // Temporary: the API only accepts HTTP Basic until login and JWT are built
                        .addSecuritySchemes(basicSchemeName,
                                new SecurityScheme()
                                        .name(basicSchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(basicSchemeName));
    }

}
