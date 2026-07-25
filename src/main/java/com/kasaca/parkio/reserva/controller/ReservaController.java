package com.kasaca.parkio.reserva.controller;

import com.kasaca.parkio.reserva.dto.ReservaRequest;
import com.kasaca.parkio.reserva.dto.ReservaResponse;
import com.kasaca.parkio.reserva.service.ReservaService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Reservas",
        description = "Endpoints para crear y consultar reservas temporales de cajones"
)
@SecurityRequirement(name = "bearerAuth")
public class ReservaController {

    private final ReservaService reservaService;

    /**
     * Crea una reserva temporal para el usuario autenticado.
     *
     * <p>El usuario solo envia estacionamiento, cajon y placa. El backend calcula
     * el codigo de reserva, la fecha de expiracion y cambia el cajon a RESERVADO.</p>
     *
     * @param request datos necesarios para crear la reserva
     * @param jwt JWT validado por Spring Security con el claim usuarioId
     * @param httpRequest solicitud HTTP usada para construir ApiResponse con transactionId
     * @return respuesta estandarizada con la reserva creada
     */
    @Operation(
            summary = "Crear reserva",
            description = """
                    Crea una reserva temporal para el usuario autenticado.
                    El cajon debe estar LIBRE y no debe tener una reserva vigente.
                    Requiere rol USER.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Reserva creada correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Datos invalidos en la solicitud"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticacion requerida o token invalido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permiso para crear reservas"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Usuario, estacionamiento o cajon no encontrado"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "El cajon no esta disponible o ya tiene una reserva vigente"
            )
    })
    @PreAuthorize("hasRole('USER')")
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<ReservaResponse>> crearReserva(
            @Valid @RequestBody ReservaRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info("INICIO - Creacion de reserva");

        Long usuarioId = getUsuarioId(jwt);
        ReservaResponse response = reservaService.crearReserva(usuarioId, request);

        log.info("FIN - Creacion de reserva");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.of(
                                httpRequest,
                                HttpStatus.CREATED.value(),
                                "Reserva creada correctamente",
                                response
                        )
                );
    }

    /**
     * Consulta las reservas activas del usuario autenticado.
     *
     * <p>Este endpoint esta pensado para que el cliente final vea sus propias reservas
     * sin recibir informacion de otros usuarios.</p>
     *
     * @param pageable parametros de paginacion y ordenamiento
     * @param jwt JWT validado por Spring Security con el claim usuarioId
     * @param httpRequest solicitud HTTP usada para construir ApiResponse con transactionId
     * @return respuesta estandarizada con las reservas del usuario autenticado
     */
    @Operation(
            summary = "Consultar mis reservas",
            description = """
                    Devuelve las reservas activas del usuario autenticado de forma paginada.
                    Requiere rol USER.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Reservas consultadas correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticacion requerida o token invalido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permiso para consultar reservas"
            )
    })
    @PreAuthorize("hasRole('USER')")
    @GetMapping(
            value = "/mis-reservas",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<PageResponse<ReservaResponse>>> getMisReservas(
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info("INICIO - Consulta de reservas del usuario autenticado");

        Long usuarioId = getUsuarioId(jwt);
        PageResponse<ReservaResponse> response = reservaService.getReservasByUsuario(usuarioId, pageable);

        log.info("FIN - Consulta de reservas del usuario autenticado");

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Reservas consultadas correctamente",
                        response
                )
        );
    }

    /**
     * Consulta una reserva activa por su codigo publico.
     *
     * <p>Este endpoint sirve para que el operador valide la reserva cuando el cliente
     * llega al estacionamiento y muestra su codigo.</p>
     *
     * @param codigo codigo publico de la reserva
     * @param httpRequest solicitud HTTP usada para construir ApiResponse con transactionId
     * @return respuesta estandarizada con los datos de la reserva encontrada
     */
    @Operation(
            summary = "Consultar reserva por codigo",
            description = """
                    Devuelve una reserva activa usando su codigo publico.
                    Este endpoint sera usado principalmente por el operador cuando el cliente llegue.
                    Requiere rol ADMIN, OWNER u OPERADOR.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Reserva consultada correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticacion requerida o token invalido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permiso para consultar reservas por codigo"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Reserva no encontrada"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'OPERADOR')")
    @GetMapping(
            value = "/codigo/{codigo}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<ReservaResponse>> getReservaByCodigo(
            @Parameter(description = "Codigo publico de la reserva", example = "RSV-A1B2C3D4")
            @PathVariable String codigo,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info("INICIO - Consulta de reserva por codigo");

        ReservaResponse response = reservaService.getReservaByCodigo(codigo);

        log.info("FIN - Consulta de reserva por codigo");

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Reserva consultada correctamente",
                        response
                )
        );
    }

    /**
     * Consulta una reserva activa por identificador interno.
     *
     * <p>Por seguridad, esta consulta queda inicialmente limitada a ADMIN.
     * El cliente final usara /mis-reservas y el operador usara /codigo/{codigo}.</p>
     *
     * @param reservaId identificador interno de la reserva
     * @param httpRequest solicitud HTTP usada para construir ApiResponse con transactionId
     * @return respuesta estandarizada con los datos de la reserva encontrada
     */
    @Operation(
            summary = "Consultar reserva por ID",
            description = """
                    Devuelve una reserva activa por su identificador interno.
                    Requiere rol ADMIN.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Reserva consultada correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticacion requerida o token invalido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permiso para consultar la reserva"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Reserva no encontrada"
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(
            value = "/{reservaId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<ReservaResponse>> getReservaById(
            @Parameter(description = "Identificador interno de la reserva", example = "1")
            @PathVariable Long reservaId,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info("INICIO - Consulta de reserva por ID");

        ReservaResponse response = reservaService.getReservaById(reservaId);

        log.info("FIN - Consulta de reserva por ID");

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Reserva consultada correctamente",
                        response
                )
        );
    }

    /**
     * Cancela una reserva activa del usuario autenticado.
     *
     * <p>Solo el cliente dueño de la reserva puede cancelarla. La reserva debe estar
     * en estado CREADA y no debe haber expirado. Al cancelarse, el cajon reservado
     * puede volver a LIBRE.</p>
     *
     * @param reservaId identificador interno de la reserva
     * @param jwt JWT validado por Spring Security con el claim usuarioId
     * @param httpRequest solicitud HTTP usada para construir ApiResponse con transactionId
     * @return respuesta estandarizada con la reserva cancelada
     */
    @Operation(
            summary = "Cancelar reserva",
            description = """
                    Cancela una reserva activa del usuario autenticado.
                    Solo se pueden cancelar reservas propias en estado CREADA y vigentes.
                    Requiere rol USER.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Reserva cancelada correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticacion requerida o token invalido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permiso para cancelar reservas"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Reserva no encontrada"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "La reserva no puede cancelarse por su estado actual"
            )
    })
    @PreAuthorize("hasRole('USER')")
    @PatchMapping(
            value = "/{reservaId}/cancelar",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<ReservaResponse>> cancelarReserva(
            @Parameter(description = "Identificador interno de la reserva", example = "1")
            @PathVariable Long reservaId,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info("INICIO - Cancelacion de reserva");

        Long usuarioId = getUsuarioId(jwt);
        ReservaResponse response = reservaService.cancelarReserva(reservaId, usuarioId);

        log.info("FIN - Cancelacion de reserva");

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Reserva cancelada correctamente",
                        response
                )
        );
    }

    /**
     * Obtiene el identificador del usuario autenticado desde el JWT.
     *
     * <p>El claim usuarioId se agrega al token durante el login y permite asociar
     * la reserva al usuario real sin recibir ese dato desde el frontend.</p>
     */
    private Long getUsuarioId(Jwt jwt) {
        Number usuarioId = jwt.getClaim("usuarioId");
        return usuarioId.longValue();
    }
}
