package org.example.gudyeeday.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("GudYeeDay API")
                .description("GudYeeDay API 명세서")
                .version("v1.0.0");

        String securitySchemeName = "bearerAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);

        Components components = new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                );

        Server localServer = new Server().url("/").description("로컬 서버");
//        Server prodServer = new Server().url("https://api.도메인.com").description("배포 서버");

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components)
//                .servers(List.of(localServer, prodServer));
                .servers(List.of(localServer));
    }

}

