package com.resumeai.template_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8085")
                                .description("Development Server"),
                        new Server()
                                .url("http://api.resumeai.com")
                                .description("Production Server")
                ))
                .info(new Info()
                        .title("Template Service API")
                        .description("Resume Template Management Microservice")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Resume AI Team")
                                .url("https://resumeai.com")
                                .email("support@resumeai.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}

