package com.fintech.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String DEFAULT_INCLUDE_PATTERN = "/api/.*";

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .securityContexts(Collections.singletonList(securityContext()))
                .securitySchemes(Collections.singletonList(apiKey()))
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.fintech.payment"))
                .paths(PathSelectors.ant("/api/**"))
                .build()
                .useDefaultResponseMessages(false)
                .forCodeGeneration(true);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Payment API")
                .description("Fintech Payment Processing API - Provides endpoints for account management, "
                        + "wallet operations, fund transfers, payment processing, and transaction history. "
                        + "Supports idempotent operations, webhook notifications, and comprehensive audit logging.")
                .version("1.0.0")
                .contact(new Contact(
                        "Payment API Team",
                        "https://payment-api.fintech.com",
                        "api-support@fintech.com"
                ))
                .license("Proprietary")
                .licenseUrl("https://payment-api.fintech.com/license")
                .build();
    }

    private ApiKey apiKey() {
        return new ApiKey("JWT", AUTHORIZATION_HEADER, "header");
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                .operationSelector(oc -> oc.requestMappingPattern().matches(DEFAULT_INCLUDE_PATTERN))
                .build();
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[]{authorizationScope};
        return Collections.singletonList(new SecurityReference("JWT", authorizationScopes));
    }
}
