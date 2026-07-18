package com.kasaca.parkio.auth.controller;

import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.auth.dto.AuthResponse;
import com.kasaca.parkio.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST del modulo de autenticacion.
 *
 * <p>Expone las operaciones publicas necesarias para obtener un JWT.</p>
 */
@Tag(
        name = "Auth",
        description = "Operaciones publicas de autenticacion y emision de JWT"
)
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Autentica a un usuario mediante correo y contrasena.
     *
     * <p>Si las credenciales son validas, devuelve un token JWT que debe usarse
     * en las siguientes peticiones protegidas.</p>
     */
    @Operation(
            summary = "Iniciar sesion",
            description = "Valida el correo y la contrasena de un usuario activo. Si las credenciales son correctas, devuelve un token JWT para consumir endpoints protegidos."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login exitoso. Devuelve el JWT, el tipo de token y el tiempo de expiracion.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(
                                    name = "Login exitoso",
                                    value = """
                                            {
                                              "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                              "tokenType": "Bearer",
                                              "expiresIn": 3600
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Solicitud invalida. Ocurre cuando el correo o la contrasena no cumplen las validaciones.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Datos invalidos",
                                    value = """
                                            {
                                              "timestamp": "2026-07-18T15:40:52.9543591",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Error de validacion",
                                              "transactionId": "dcc83d2a-8bc9-4857-bdb6-5c7d936d8915",
                                              "path": "/api/v1/auth/login",
                                              "validationErrors": {
                                                "email": "El correo electronico debe tener un formato valido"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales invalidas o usuario inactivo.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "No autorizado",
                                    value = """
                                            {
                                              "timestamp": "2026-07-18T15:40:52.9543591",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Credenciales invalidas",
                                              "transactionId": "dcc83d2a-8bc9-4857-bdb6-5c7d936d8915",
                                              "path": "/api/v1/auth/login",
                                              "validationErrors": {}
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }
}
