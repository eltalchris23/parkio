package com.kasaca.parkio.tarifa.controller;

import com.kasaca.parkio.shared.dto.ApiResponse;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoRequest;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoResponse;
import com.kasaca.parkio.tarifa.service.TarifaEstacionamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Controlador REST del modulo de tarifas de estacionamiento.
 *
 * <p>Expone operaciones para consultar, crear, actualizar y eliminar logicamente
 * la configuracion de cobro asociada a un estacionamiento.</p>
 */
@Tag(
        name = "Tarifas",
        description = "Administracion de tarifas por estacionamiento"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/tarifas")
@RequiredArgsConstructor
@Slf4j
public class TarifaEstacionamientoController {

    private final TarifaEstacionamientoService tarifaEstacionamientoService;

    /**
     * Consulta la tarifa activa configurada para un estacionamiento.
     *
     * <p>ADMIN puede consultar cualquier tarifa. OWNER solo puede consultar
     * tarifas de estacionamientos propios.</p>
     *
     * @param estacionamientoId identificador del estacionamiento consultado
     * @param jwt JWT validado por Spring Security con los datos del usuario autenticado
     * @param request solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con la tarifa activa del estacionamiento
     */
    @Operation(
            summary = "Consultar tarifa por estacionamiento",
            description = "Consulta la tarifa activa asociada a un estacionamiento. Requiere rol ADMIN u OWNER."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tarifa consultada correctamente.",
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
                    description = "No autorizado. El usuario autenticado no tiene rol ADMIN u OWNER, o no tiene alcance sobre el estacionamiento.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Tarifa o estacionamiento no encontrado.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @GetMapping("/estacionamiento/{estacionamientoId}")
    public ResponseEntity<ApiResponse<TarifaEstacionamientoResponse>> getTarifaByEstacionamientoId(
            @Parameter(description = "Identificador del estacionamiento", example = "1")
            @PathVariable Long estacionamientoId,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        log.info("INICIO - Consulta de tarifa por estacionamiento");

        TarifaEstacionamientoResponse response =
                tarifaEstacionamientoService.getTarifaByEstacionamientoId(estacionamientoId, jwt);

        log.info("FIN - Consulta de tarifa por estacionamiento");

        return ResponseEntity.ok(
                ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Tarifa consultada correctamente",
                        response
                )
        );
    }

    /**
     * Crea la tarifa activa de un estacionamiento.
     *
     * <p>ADMIN puede crear tarifas para cualquier estacionamiento. OWNER solo
     * puede crear tarifas para estacionamientos propios.</p>
     *
     * @param request datos necesarios para crear la tarifa
     * @param jwt JWT validado por Spring Security con los datos del usuario autenticado
     * @param httpRequest solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con la tarifa creada y estado HTTP 201
     */
    @Operation(
            summary = "Crear tarifa",
            description = "Crea la tarifa activa de un estacionamiento. Requiere rol ADMIN u OWNER."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Tarifa creada correctamente.",
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
                    description = "No autorizado. El usuario autenticado no tiene rol ADMIN u OWNER, o no tiene alcance sobre el estacionamiento.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflicto. Ya existe una tarifa activa para el estacionamiento.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @PostMapping
    public ResponseEntity<ApiResponse<TarifaEstacionamientoResponse>> addTarifa(
            @Valid @RequestBody TarifaEstacionamientoRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info("INICIO - Creacion de tarifa");

        TarifaEstacionamientoResponse response =
                tarifaEstacionamientoService.addTarifa(request, jwt);

        log.info("FIN - Creacion de tarifa");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.of(
                                httpRequest,
                                HttpStatus.CREATED.value(),
                                "Tarifa creada correctamente",
                                response
                        )
                );
    }

    /**
     * Actualiza la tarifa activa de un estacionamiento.
     *
     * <p>El identificador de la ruta representa el estacionamiento cuya tarifa
     * se actualizara. El service valida que coincida con el estacionamientoId
     * enviado en el body.</p>
     *
     * @param estacionamientoId identificador del estacionamiento cuya tarifa se actualizara
     * @param request datos actualizados de la tarifa
     * @param jwt JWT validado por Spring Security con los datos del usuario autenticado
     * @param httpRequest solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con la tarifa actualizada
     */
    @Operation(
            summary = "Actualizar tarifa",
            description = "Actualiza la tarifa activa de un estacionamiento. Requiere rol ADMIN u OWNER."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tarifa actualizada correctamente.",
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
                    description = "No autorizado. El usuario autenticado no tiene rol ADMIN u OWNER, o no tiene alcance sobre el estacionamiento.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Tarifa o estacionamiento no encontrado.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflicto. El estacionamiento de la ruta no coincide con el estacionamiento del body.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @PutMapping("/estacionamiento/{estacionamientoId}")
    public ResponseEntity<ApiResponse<TarifaEstacionamientoResponse>> updateTarifa(
            @Parameter(description = "Identificador del estacionamiento", example = "1")
            @PathVariable Long estacionamientoId,
            @Valid @RequestBody TarifaEstacionamientoRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info("INICIO - Actualizacion de tarifa");

        TarifaEstacionamientoResponse response =
                tarifaEstacionamientoService.updateTarifa(estacionamientoId, request, jwt);

        log.info("FIN - Actualizacion de tarifa");

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Tarifa actualizada correctamente",
                        response
                )
        );
    }

    /**
     * Elimina logicamente la tarifa activa de un estacionamiento.
     *
     * <p>La eliminacion no borra el registro fisicamente; solamente cambia
     * su estado activo a false desde la capa de servicio.</p>
     *
     * @param estacionamientoId identificador del estacionamiento cuya tarifa se desactivara
     * @param jwt JWT validado por Spring Security con los datos del usuario autenticado
     */
    @Operation(
            summary = "Eliminar tarifa logicamente",
            description = "Desactiva logicamente la tarifa activa de un estacionamiento. Requiere rol ADMIN u OWNER."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Tarifa eliminada logicamente correctamente.",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Falta token JWT o el token no es valido.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "No autorizado. El usuario autenticado no tiene rol ADMIN u OWNER, o no tiene alcance sobre el estacionamiento.",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Tarifa o estacionamiento no encontrado.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @DeleteMapping("/estacionamiento/{estacionamientoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTarifa(
            @Parameter(description = "Identificador del estacionamiento", example = "1")
            @PathVariable Long estacionamientoId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("INICIO - Eliminacion logica de tarifa");

        tarifaEstacionamientoService.deleteTarifa(estacionamientoId, jwt);

        log.info("FIN - Eliminacion logica de tarifa");
    }
}
