package com.kasaca.parkio.rol.controller;

import com.kasaca.parkio.rol.dto.RolRequest;
import com.kasaca.parkio.rol.dto.RolResponse;
import com.kasaca.parkio.rol.service.RolService;
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
 * Controlador REST del modulo de roles.
 *
 * <p>Expone operaciones administrativas para consultar, crear, actualizar
 * y eliminar logicamente roles del sistema.</p>
 */
@Tag(
        name = "Roles",
        description = "Administracion de roles del sistema"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class RolController {

    private final RolService rolService;

    /**
     * Lista los roles activos registrados en el sistema usando paginacion y
     * ordenamiento recibidos por query params como page, size y sort.
     */
    @Operation(
            summary = "Listar roles",
            description = "Consulta de forma paginada los roles activos registrados en el sistema. Requiere rol ADMIN."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Roles consultados correctamente.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Listado paginado de roles",
                                    value = """
                                            {
                                              "timestamp": "2026-07-18T16:30:00",
                                              "status": 200,
                                              "message": "Roles consultados correctamente",
                                              "transactionId": "dcc83d2a-8bc9-4857-bdb6-5c7d936d8915",
                                              "data": {
                                                "content": [
                                                  {
                                                    "id": 1,
                                                    "nombre": "ADMIN",
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
                    description = "No autorizado. El usuario autenticado no tiene rol ADMIN.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RolResponse>>> getRoles(
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        log.info("getRoles - Controller");
        PageResponse<RolResponse> roles = rolService.getRoles(pageable);

        return ResponseEntity.ok(
                ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Roles consultados correctamente",
                        roles
                )
        );
    }

    /**
     * Consulta un rol activo por su identificador y devuelve una respuesta estandarizada.
     *
     * @param rolId identificador del rol consultado
     * @param request solicitud HTTP usada para obtener o generar el transactionId
     * @return respuesta estandarizada con los datos del rol encontrado
     */
    @Operation(
            summary = "Consultar rol por id",
            description = "Consulta un rol activo por su identificador. Si el rol esta inactivo o no existe, se responde 404. Requiere rol ADMIN."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rol consultado correctamente.",
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
                    description = "No autorizado. El usuario autenticado no tiene rol ADMIN.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Rol no encontrado.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/{rolId}")
    public ResponseEntity<ApiResponse<RolResponse>> getRol(
            @Parameter(description = "Identificador del rol", example = "1")
            @PathVariable Long rolId,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        RolResponse response = rolService.getRol(rolId);

        return ResponseEntity.ok(
                ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Rol consultado correctamente",
                        response
                )
        );
    }

    /**
     * Crea un nuevo rol validando los datos recibidos y devuelve una respuesta estandarizada.
     *
     * @param request datos necesarios para crear el rol
     * @param httpRequest solicitud HTTP usada para obtener o generar el transactionId
     * @return respuesta estandarizada con el rol creado y estado HTTP 201
     */
    @Operation(
            summary = "Crear rol",
            description = "Crea un nuevo rol del sistema. Valida datos de entrada y evita nombres duplicados. Requiere rol ADMIN."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Rol creado correctamente.",
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
                    responseCode = "409",
                    description = "Conflicto. Ya existe un rol con el mismo nombre.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<RolResponse>> addRol(
            @Valid @RequestBody RolRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        RolResponse response = rolService.addRol(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.of(
                                httpRequest,
                                HttpStatus.CREATED.value(),
                                "Rol creado correctamente",
                                response
                        )
                );
    }

    /**
     * Actualiza un rol activo existente usando el identificador de la ruta y los
     * datos validados del cuerpo de la solicitud, devolviendo una respuesta estandarizada.
     *
     * @param rolId identificador del rol que se actualizara
     * @param request datos actualizados del rol
     * @param httpRequest solicitud HTTP usada para obtener o generar el transactionId
     * @return respuesta estandarizada con el rol actualizado
     */
    @Operation(
            summary = "Actualizar rol",
            description = "Actualiza un rol activo existente. Valida datos de entrada y evita nombres duplicados. Requiere rol ADMIN."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rol actualizado correctamente.",
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
                    description = "Rol no encontrado.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflicto. Ya existe otro rol con el mismo nombre.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PutMapping("/{rolId}")
    public ResponseEntity<ApiResponse<RolResponse>> updateRol(
            @Parameter(description = "Identificador del rol", example = "1")
            @PathVariable Long rolId,
            @Valid @RequestBody RolRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        RolResponse response = rolService.updateRol(rolId, request);

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Rol actualizado correctamente",
                        response
                )
        );
    }

    /**
     * Elimina logicamente un rol activo cambiando su estado a inactivo desde la
     * capa de servicio.
     */
    @Operation(
            summary = "Eliminar rol logicamente",
            description = "Desactiva un rol activo mediante borrado logico. El registro permanece en base de datos con activo=false. Requiere rol ADMIN."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Rol eliminado logicamente correctamente.",
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
                    description = "Rol no encontrado.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @DeleteMapping("/{rolId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRol(
            @Parameter(description = "Identificador del rol", example = "1")
            @PathVariable Long rolId
    ) {
        rolService.deleteRol(rolId);
    }
}
