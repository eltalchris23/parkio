package com.kasaca.parkio.estacionamiento.controller;

import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;
import com.kasaca.parkio.estacionamiento.service.EstacionamientoService;
import com.kasaca.parkio.shared.dto.ApiResponse;
import com.kasaca.parkio.shared.dto.PageResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST del módulo de estacionamientos.
 *
 * <p>Expone operaciones para consultar estacionamientos y operaciones
 * administrativas para crearlos, actualizarlos o eliminarlos lógicamente.</p>
 */
@Tag(
        name = "Estacionamientos",
        description = "Consulta y administración de estacionamientos"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/estacionamientos")
@RequiredArgsConstructor
@Slf4j
public class EstacionamientoController {

    private final EstacionamientoService estacionamientoService;

    /**
     * Lista los estacionamientos activos de forma paginada según el alcance
     * del usuario autenticado.
     *
     * <p>El JWT recibido ya fue validado por Spring Security y se usa para que
     * la capa de servicio determine si el usuario ve todos los registros o solo
     * los estacionamientos donde es owner.</p>
     */
    @Operation(
            summary = "Listar estacionamientos",
            description = "Consulta de forma paginada los estacionamientos activos. ADMIN ve todo, OWNER ve los suyos, OPERADOR ve solo estacionamientos asignados y USER conserva la consulta permitida actual."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Estacionamientos consultados correctamente.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Listado paginado de estacionamientos",
                                    value = """
                                            {
                                              "timestamp": "2026-07-18T16:30:00",
                                              "status": 200,
                                              "message": "Estacionamientos consultados correctamente",
                                              "transactionId": "dcc83d2a-8bc9-4857-bdb6-5c7d936d8915",
                                              "data": {
                                                "content": [
                                                  {
                                                    "id": 1,
                                                    "nombre": "Estacionamiento Centro",
                                                    "descripcion": "Estacionamiento ubicado en zona centro",
                                                    "latitud": 19.43260800,
                                                    "longitud": -99.13320900,
                                                    "ownerId": 2,
                                                    "activo": true,
                                                    "fechaCreacion": "2026-07-18T10:00:00"
                                                  }
                                                ],
                                                "page": 0,
                                                "size": 10,
                                                "totalElements": 1,
                                                "totalPages": 1,
                                                "last": true
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Falta token JWT o el token no es válido.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "No autorizado. El usuario autenticado no tiene un rol permitido.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'OPERADOR', 'USER')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EstacionamientoResponse>>> getEstacionamientos(
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        log.info("INICIO - Listado de estacionamientos");
        PageResponse<EstacionamientoResponse> estacionamientos =
                estacionamientoService.getEstacionamientos(pageable, jwt);
        log.info("FIN - Listado de estacionamientos");

        return ResponseEntity.ok(
                ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Estacionamientos consultados correctamente",
                        estacionamientos
                )
        );
    }

    /**
     * Consulta un estacionamiento activo por identificador respetando el alcance
     * del usuario autenticado y devuelve una respuesta estandarizada.
     *
     * @param estacionamientoId identificador del estacionamiento consultado
     * @param jwt JWT validado por Spring Security con claims del usuario autenticado
     * @param request solicitud HTTP usada para obtener o generar el transactionId
     * @return respuesta estandarizada con los datos del estacionamiento encontrado
     */
    @Operation(
            summary = "Consultar estacionamiento por id",
            description = "Consulta un estacionamiento activo por su identificador. ADMIN puede consultar cualquiera, OWNER solo los propios, OPERADOR solo los asignados y USER conserva la consulta permitida actual."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Estacionamiento consultado correctamente.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Falta token JWT o el token no es válido.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "No autorizado. El usuario autenticado no tiene un rol permitido.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Estacionamiento no encontrado.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'OPERADOR', 'USER')")
    @GetMapping("/{estacionamientoId}")
    public ResponseEntity<ApiResponse<EstacionamientoResponse>> getEstacionamientoById(
            @Parameter(description = "Identificador del estacionamiento", example = "1")
            @PathVariable Long estacionamientoId,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        EstacionamientoResponse response =
                estacionamientoService.getEstacionamientoById(estacionamientoId, jwt);

        return ResponseEntity.ok(
                ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Estacionamiento consultado correctamente",
                        response
                )
        );
    }

    /**
     * Crea un nuevo estacionamiento con datos validados.
     *
     * <p>ADMIN puede crear estacionamientos administrativos. OWNER puede crear
     * estacionamientos que quedan asociados a su usuario autenticado.</p>
     *
     * @param request datos necesarios para crear el estacionamiento
     * @param jwt JWT validado por Spring Security con claims del usuario autenticado
     * @param httpRequest solicitud HTTP usada para obtener o generar el transactionId
     * @return respuesta estandarizada con el estacionamiento creado y estado HTTP 201
     */
    @Operation(
            summary = "Crear estacionamiento",
            description = "Crea un nuevo estacionamiento con nombre, descripción opcional, latitud y longitud. Requiere rol ADMIN u OWNER."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Estacionamiento creado correctamente.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Solicitud inválida por errores de validación.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Falta token JWT o el token no es válido.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "No autorizado. El usuario autenticado no tiene rol ADMIN u OWNER.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @PostMapping
    public ResponseEntity<ApiResponse<EstacionamientoResponse>> addEstacionamiento(
            @Valid @RequestBody EstacionamientoRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        EstacionamientoResponse response = estacionamientoService.addEstacionamiento(request, jwt);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.of(
                                httpRequest,
                                HttpStatus.CREATED.value(),
                                "Estacionamiento creado correctamente",
                                response
                        )
                );
    }

    /**
     * Actualiza un estacionamiento activo existente respetando el alcance del
     * usuario autenticado.
     *
     * @param estacionamientoId identificador del estacionamiento que se actualizará
     * @param request datos actualizados del estacionamiento
     * @param jwt JWT validado por Spring Security con claims del usuario autenticado
     * @param httpRequest solicitud HTTP usada para obtener o generar el transactionId
     * @return respuesta estandarizada con el estacionamiento actualizado
     */
    @Operation(
            summary = "Actualizar estacionamiento",
            description = "Actualiza un estacionamiento activo existente. ADMIN puede actualizar cualquiera y OWNER solo los propios."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Estacionamiento actualizado correctamente.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Solicitud inválida por errores de validación.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Falta token JWT o el token no es válido.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "No autorizado. El usuario autenticado no tiene rol ADMIN u OWNER.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Estacionamiento no encontrado.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @PutMapping("/{estacionamientoId}")
    public ResponseEntity<ApiResponse<EstacionamientoResponse>> updateEstacionamiento(
            @Parameter(description = "Identificador del estacionamiento", example = "1")
            @PathVariable Long estacionamientoId,
            @Valid @RequestBody EstacionamientoRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        EstacionamientoResponse response =
                estacionamientoService.updateEstacionamiento(estacionamientoId, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Estacionamiento actualizado correctamente",
                        response
                )
        );
    }

    /**
     * Elimina lógicamente un estacionamiento activo respetando el alcance del
     * usuario autenticado.
     *
     * <p>La capa de servicio también desactiva sus cajones activos asociados.</p>
     */
    @Operation(
            summary = "Eliminar estacionamiento lógicamente",
            description = "Desactiva lógicamente un estacionamiento activo y también desactiva sus cajones activos asociados. ADMIN puede eliminar cualquiera y OWNER solo los propios."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Estacionamiento eliminado lógicamente correctamente.",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Falta token JWT o el token no es válido.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "No autorizado. El usuario autenticado no tiene rol ADMIN u OWNER.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Estacionamiento no encontrado.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @DeleteMapping("/{estacionamientoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEstacionamiento(
            @Parameter(description = "Identificador del estacionamiento", example = "1")
            @PathVariable Long estacionamientoId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        estacionamientoService.deleteEstacionamiento(estacionamientoId, jwt);
    }
}
