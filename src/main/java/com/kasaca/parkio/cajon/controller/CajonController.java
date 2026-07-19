package com.kasaca.parkio.cajon.controller;

import com.kasaca.parkio.cajon.dto.CajonEstadoRequest;
import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.service.CajonService;
import com.kasaca.parkio.shared.dto.ApiResponse;
import com.kasaca.parkio.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cajones")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Cajones",
        description = "Endpoints para consultar y administrar cajones de estacionamiento"
)
@SecurityRequirement(name = "bearerAuth")
public class CajonController {

    private final CajonService cajonService;

    /**
     * Lista cajones activos de forma paginada.
     *
     * <p>Si se recibe estacionamientoId, filtra los cajones por estacionamiento.
     * El JWT ya fue validado por Spring Security y se usa en la capa de servicio
     * para limitar a OWNER a los cajones de sus propios estacionamientos.</p>
     *
     * @param estacionamientoId identificador opcional del estacionamiento usado para filtrar cajones
     * @param pageable datos de paginación y ordenamiento enviados como page, size y sort
     * @param jwt JWT validado por Spring Security con claims del usuario autenticado
     * @param request solicitud HTTP usada para construir la respuesta estandarizada con transactionId
     * @return respuesta estandarizada con la página de cajones encontrados
     */
    @Operation(
            summary = "Listar cajones activos",
            description = """
                    Devuelve cajones activos de forma paginada.
                    Si se envía estacionamientoId, devuelve únicamente los cajones activos de ese estacionamiento.
                    Requiere rol ADMIN, OWNER, OPERADOR o USER.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Cajones consultados correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para consultar cajones"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Estacionamiento no encontrado cuando se consulta por estacionamientoId"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'OPERADOR', 'USER')")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<CajonResponse>>> getCajones(
            @Parameter(
                    description = "Identificador opcional del estacionamiento para filtrar cajones",
                    example = "1"
            )
            @RequestParam(required = false) Long estacionamientoId,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        log.info("INICIO - Listado de cajones");

        PageResponse<CajonResponse> cajones;

        if (estacionamientoId == null) {
            cajones = cajonService.getCajones(pageable, jwt);
        } else {
            cajones = cajonService.getCajonesByEstacionamientoId(
                    estacionamientoId,
                    pageable,
                    jwt
            );
        }

        log.info("FIN - Listado de cajones");

        return ResponseEntity.ok(
                ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Cajones consultados correctamente",
                        cajones
                )
        );
    }

    /**
     * Consulta un cajón activo por su identificador respetando el alcance del
     * usuario autenticado.
     *
     * @param cajonId identificador del cajón que se desea consultar
     * @param jwt JWT validado por Spring Security con claims del usuario autenticado
     * @param request solicitud HTTP usada para construir la respuesta estandarizada con transactionId
     * @return respuesta estandarizada con los datos del cajón encontrado
     */
    @Operation(
            summary = "Consultar cajón por identificador",
            description = """
                    Devuelve la información de un cajón activo.
                    Si el cajón no existe o está inactivo, la API responde 404.
                    Requiere rol ADMIN, OWNER, OPERADOR o USER.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Cajón consultado correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para consultar cajones"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Cajón no encontrado"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'OPERADOR', 'USER')")
    @GetMapping(value = "/{cajonId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CajonResponse>> getCajon(
            @Parameter(description = "Identificador del cajón", example = "1")
            @PathVariable Long cajonId,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        CajonResponse response = cajonService.getCajon(cajonId, jwt);

        return ResponseEntity.ok(
                ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Cajon consultado correctamente",
                        response
                )
        );
    }

    /**
     * Crea un cajón dentro de un estacionamiento existente.
     *
     * <p>ADMIN puede crear cajones en cualquier estacionamiento. OWNER solo
     * puede crear cajones en estacionamientos donde sea dueño.</p>
     *
     * @param request datos necesarios para crear el cajón
     * @param jwt JWT validado por Spring Security con claims del usuario autenticado
     * @param httpRequest solicitud HTTP usada para construir la respuesta estandarizada con transactionId
     * @return respuesta estandarizada con el cajón creado y estado HTTP 201
     */
    @Operation(
            summary = "Crear cajón",
            description = """
                    Crea un nuevo cajón asociado a un estacionamiento existente.
                    El número del cajón no debe duplicarse dentro del mismo estacionamiento.
                    Requiere rol ADMIN u OWNER.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Cajón creado correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos en la solicitud"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para crear cajones"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Estacionamiento no encontrado"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Ya existe un cajón con el mismo número en el estacionamiento"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<CajonResponse>> addCajon(
            @Valid @RequestBody CajonRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        CajonResponse response = cajonService.addCajon(request, jwt);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.of(
                                httpRequest,
                                HttpStatus.CREATED.value(),
                                "Cajon creado correctamente",
                                response
                        )
                );
    }

    /**
     * Actualiza los datos principales de un cajón existente.
     *
     * <p>ADMIN puede actualizar cualquier cajón. OWNER solo puede actualizar
     * cajones de sus propios estacionamientos.</p>
     *
     * @param cajonId identificador del cajón que se desea actualizar
     * @param request datos actualizados del cajón
     * @param jwt JWT validado por Spring Security con claims del usuario autenticado
     * @param httpRequest solicitud HTTP usada para construir la respuesta estandarizada con transactionId
     * @return respuesta estandarizada con el cajón actualizado
     */
    @Operation(
            summary = "Actualizar cajón",
            description = """
                    Actualiza los datos principales de un cajón existente.
                    No se usa para cambiar únicamente el estado operativo; para eso existe PATCH /cajones/{cajonId}/estado.
                    Requiere rol ADMIN u OWNER.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Cajón actualizado correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos en la solicitud"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para actualizar cajones"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Cajón o estacionamiento no encontrado"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Ya existe un cajón con el mismo número en el estacionamiento"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @PutMapping(
            value = "/{cajonId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<CajonResponse>> updateCajon(
            @Parameter(description = "Identificador del cajón", example = "1")
            @PathVariable Long cajonId,
            @Valid @RequestBody CajonRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        CajonResponse response = cajonService.updateCajon(cajonId, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Cajon actualizado correctamente",
                        response
                )
        );
    }

    /**
     * Cambia únicamente el estado operativo de un cajón.
     *
     * <p>ADMIN y OWNER pueden cambiar estado dentro de su alcance. OPERADOR
     * conserva la regla actual y más adelante se limitará por asignación a
     * estacionamientos.</p>
     *
     * @param cajonId identificador del cajón que cambiará de estado
     * @param request nuevo estado operativo del cajón
     * @param jwt JWT validado por Spring Security con claims del usuario autenticado
     * @param httpRequest solicitud HTTP usada para construir la respuesta estandarizada con transactionId
     * @return respuesta estandarizada con el cajón con estado actualizado
     */
    @Operation(
            summary = "Cambiar estado de cajón",
            description = """
                    Cambia únicamente el estado operativo del cajón.
                    Este endpoint está pensado para operación diaria del estacionamiento.
                    Requiere rol ADMIN, OWNER u OPERADOR.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Estado del cajón actualizado correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Estado inválido en la solicitud"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para cambiar el estado del cajón"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Cajón no encontrado"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'OPERADOR')")
    @PatchMapping(
            value = "/{cajonId}/estado",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<CajonResponse>> updateEstado(
            @Parameter(description = "Identificador del cajón", example = "1")
            @PathVariable Long cajonId,
            @Valid @RequestBody CajonEstadoRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        CajonResponse response = cajonService.updateEstado(cajonId, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Estado del cajon actualizado correctamente",
                        response
                )
        );
    }

    /**
     * Elimina lógicamente un cajón mediante su identificador.
     *
     * <p>ADMIN puede eliminar cualquier cajón. OWNER solo puede eliminar cajones
     * de sus propios estacionamientos.</p>
     *
     * @param cajonId identificador del cajón que se desea desactivar
     * @param jwt JWT validado por Spring Security con claims del usuario autenticado
     */
    @Operation(
            summary = "Eliminar cajón",
            description = """
                    Desactiva lógicamente un cajón cambiando su campo activo a false.
                    No elimina físicamente el registro de la base de datos.
                    Requiere rol ADMIN u OWNER.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Cajón eliminado lógicamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para eliminar cajones"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Cajón no encontrado"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @DeleteMapping("/{cajonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCajon(
            @Parameter(description = "Identificador del cajón", example = "1")
            @PathVariable Long cajonId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        cajonService.deleteCajon(cajonId, jwt);
    }
}
