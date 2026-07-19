package com.kasaca.parkio.auth.controller;

import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.auth.dto.AuthResponse;
import com.kasaca.parkio.auth.service.AuthService;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST del modulo de autenticacion.
 *
 * <p>Expone el login publico para obtener JWT y la consulta protegida del
 * usuario autenticado actual.</p>
 */
@Tag(
        name = "Auth",
        description = "Operaciones publicas de autenticacion y consulta del usuario autenticado"
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
     *
     * @param request credenciales recibidas desde el cliente
     * @return respuesta con token JWT, tipo de token y segundos de expiracion
     */
    @Operation(
            summary = "Iniciar sesion",
            description = "Valida el correo y la contrasena de un usuario activo. Si las credenciales son correctas, devuelve un token JWT para consumir endpoints protegidos."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
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

    /**
     * Consulta la informacion actual del usuario autenticado.
     *
     * <p>Spring Security valida el JWT antes de entrar al metodo. Despues, el
     * servicio usa el claim usuarioId para consultar la informacion vigente del
     * usuario en base de datos.</p>
     *
     * @param jwt token JWT ya validado y convertido por Spring Security
     * @param request solicitud HTTP usada para construir la respuesta estandarizada con transactionId
     * @return respuesta estandarizada con los datos actuales del usuario autenticado
     */
    @Operation(
            summary = "Consultar usuario autenticado",
            description = """
                    Devuelve la informacion actual del usuario autenticado.
                    Sirve para que el frontend conozca el usuario, roles y estacionamientos asignados
                    sin depender de decodificar el JWT.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Usuario autenticado consultado correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticacion requerida, token invalido o token sin usuarioId"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Usuario autenticado no encontrado o inactivo"
            )
    })
    @GetMapping("/me")
    public ResponseEntity<com.kasaca.parkio.shared.dto.ApiResponse<UsuarioResponse>> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt,

            @Parameter(hidden = true)
            HttpServletRequest request
    ) {
        UsuarioResponse response = authService.getCurrentUser(jwt);

        return ResponseEntity.ok(
                com.kasaca.parkio.shared.dto.ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Usuario autenticado consultado correctamente",
                        response
                )
        );
    }
}
