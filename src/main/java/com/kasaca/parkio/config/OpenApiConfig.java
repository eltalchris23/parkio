package com.kasaca.parkio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de OpenAPI para documentar la API REST de Parkio.
 *
 * <p>Esta clase define la informacion general del contrato y registra
 * el esquema de seguridad Bearer JWT para que Swagger UI permita probar
 * endpoints protegidos usando un token.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    /**
     * Construye la definicion principal de OpenAPI para Parkio.
     *
     * @return configuracion OpenAPI usada por Swagger UI
     */
    @Bean
    public OpenAPI parkioOpenAPI() {
        return new OpenAPI()
                // Define datos generales visibles en Swagger UI.
                .info(apiInfo())
                // Indica que los endpoints pueden usar autenticacion Bearer JWT.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                // Registra el esquema de seguridad para que Swagger muestre el boton Authorize.
                .schemaRequirement(BEARER_AUTH, bearerJwtSecurityScheme());
    }

    /**
     * Define la informacion descriptiva de la API.
     *
     * @return metadatos principales del contrato OpenAPI
     */
    private Info apiInfo() {
        return new Info()
                .title("Parkio API")
                .description("API REST para administrar usuarios, roles, estacionamientos y cajones.")
                .version("v1");
    }

    /**
     * Define el esquema Bearer JWT usado por Spring Security.
     *
     * @return esquema de seguridad para enviar Authorization: Bearer &lt;token&gt;
     */
    private SecurityScheme bearerJwtSecurityScheme() {
        return new SecurityScheme()
                // Indica que la autenticacion se envia mediante un header HTTP.
                .type(SecurityScheme.Type.HTTP)
                // Define el esquema Authorization: Bearer.
                .scheme("bearer")
                // Indica que el token esperado es JWT.
                .bearerFormat("JWT");
    }
}
