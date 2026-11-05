package com.scuec.icpc.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI icpcOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ICPC 协会管理系统 API")
                        .version("1.0.0")
                        .description("中南民族大学 ICPC 协会管理系统后端接口。统一前缀 /api/v1，鉴权方式 JWT Bearer Token。")
                        .contact(new Contact().name("Aurora 4")))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
