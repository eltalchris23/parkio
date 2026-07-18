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
 * Controlador REST del modulo de estacionamientos.
 *
 * <p>Expone operaciones para consultar estacionamientos y operaciones
 * administrativas para crearlos, actualizarlos o eliminarlos logicamente.</p>
 */
@Tag(
        name = "Estacionamientos",
        description = "Consulta y administracion de estacionamientos"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/estacionamientos")
@RequiredArgsConstructor
@Slf4j
public class EstacionamientoController {

    private final EstacionamientoService estacionamientoService;

    /**
     * Lista los estacionamientos activos de forma paginada para usuarios
     * autenticados con rol ADMIN, OPERADOR o USER.
     */
    @Operation(
            summary = "Listar estacionamientos",
            description = "Consulta de forma paginada los estacionamientos activos. Permite roles ADMIN, OPERADOR y USER."
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
                    description = "No autenticado. Falta token JWT o el token no es valido.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "No autorizado. El usuario autenticado no tiene un rol permitido.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'USER')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EstacionamientoResponse>>> getEstacionamientos(
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        log.info("INICIO - Listado de estacionamientos");
        PageResponse<EstacionamientoResponse> estacionamientos =
                estacionamientoService.getEstacionamientos(pageable);
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
     * Consulta un estacionamiento activo por identificador para usuarios
     * autenticados con rol ADMIN, OPERADOR o USER y devuelve una respuesta estandarizada.
     *
     * @param estacionamientoId identificador del estacionamiento consultado
     * @param request solicitud HTTP usada para obtener o generar el transactionId
     * @return respuesta estandarizada con los datos del estacionamiento encontrado
     */
    @Operation(
            summary = "Consultar estacionamiento por id",
            description = "Consulta un estacionamiento activo por su identificador. Si esta inactivo o no existe, se responde 404. Permite roles ADMIN, OPERADOR y USER."
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
                    description = "No autenticado. Falta token JWT o el token no es valido.",
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
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'USER')")
    @GetMapping("/{estacionamientoId}")
    public ResponseEntity<ApiResponse<EstacionamientoResponse>> getEstacionamientoById(
            @Parameter(description = "Identificador del estacionamiento", example = "1")
            @PathVariable Long estacionamientoId,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        EstacionamientoResponse response =
                estacionamientoService.getEstacionamientoById(estacionamientoId);

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
     * Crea un nuevo estacionamiento con datos validados. Esta operacion solo esta
     * permitida para usuarios con rol ADMIN y devuelve una respuesta estandarizada.
     *
     * @param request datos necesarios para crear el estacionamiento
     * @param httpRequest solicitud HTTP usada para obtener o generar el transactionId
     * @return respuesta estandarizada con el estacionamiento creado y estado HTTP 201
     */
    @Operation(
            summary = "Crear estacionamiento",
            description = "Crea un nuevo estacionamiento con nombre, descripcion opcional, latitud y longitud. Requiere rol ADMIN."
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
                    description = "Solicitud invalida por errores de validacion.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Falta token JWT o el token no es valido.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "No autorizado. El usuario autenticado no tiene rol ADMIN.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<EstacionamientoResponse>> addEstacionamiento(
            @Valid @RequestBody EstacionamientoRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        EstacionamientoResponse response = estacionamientoService.addEstacionamiento(request);

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
     * Actualiza un estacionamiento activo existente. Esta operacion solo esta
     * permitida para usuarios con rol ADMIN y devuelve una respuesta estandarizada.
     *
     * @param estacionamientoId identificador del estacionamiento que se actualizara
     * @param request datos actualizados del estacionamiento
     * @param httpRequest solicitud HTTP usada para obtener o generar el transactionId
     * @return respuesta estandarizada con el estacionamiento actualizado
     */
    @Operation(
            summary = "Actualizar estacionamiento",
            description = "Actualiza un estacionamiento activo existente. Valida nombre, descripcion, latitud y longitud. Requiere rol ADMIN."
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
                    description = "Solicitud invalida por errores de validacion.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Falta token JWT o el token no es valido.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "No autorizado. El usuario autenticado no tiene rol ADMIN.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Estacionamiento no encontrado.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{estacionamientoId}")
    public ResponseEntity<ApiResponse<EstacionamientoResponse>> updateEstacionamiento(
            @Parameter(description = "Identificador del estacionamiento", example = "1")
            @PathVariable Long estacionamientoId,
            @Valid @RequestBody EstacionamientoRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        EstacionamientoResponse response =
                estacionamientoService.updateEstacionamiento(estacionamientoId, request);

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
     * Elimina logicamente un estacionamiento activo. La capa de servicio tambien
     * desactiva sus cajones activos asociados.
     */
    @Operation(
            summary = "Eliminar estacionamiento logicamente",
            description = "Desactiva logicamente un estacionamiento activo y tambien desactiva sus cajones activos asociados. Requiere rol ADMIN."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Estacionamiento eliminado logicamente correctamente.",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Falta token JWT o el token no es valido.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "No autorizado. El usuario autenticado no tiene rol ADMIN.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Estacionamiento no encontrado.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{estacionamientoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEstacionamiento(
            @Parameter(description = "Identificador del estacionamiento", example = "1")
            @PathVariable Long estacionamientoId
    ) {
        estacionamientoService.deleteEstacionamiento(estacionamientoId);
    }
}
