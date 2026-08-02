package com.kasaca.parkio.ticket.controller;

import com.kasaca.parkio.shared.dto.ApiResponse;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.ticket.dto.TicketEntradaRequest;
import com.kasaca.parkio.ticket.dto.TicketResponse;
import com.kasaca.parkio.ticket.service.TicketService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Tickets",
        description = "Endpoints para convertir reservas vigentes en tickets de estacionamiento"
)
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final TicketService ticketService;

    /**
     * Consulta tickets activos de forma paginada segun el alcance del usuario autenticado.
     *
     * <p>ADMIN consulta todos los tickets. OWNER consulta tickets de sus estacionamientos.
     * OPERADOR consulta tickets de estacionamientos asignados. USER consulta solo sus propios tickets.</p>
     *
     * @param pageable parametros de paginacion y ordenamiento recibidos como page, size y sort
     * @param estado estado opcional para filtrar tickets ABIERTO, PENDIENTE_PAGO o CERRADO
     * @param estacionamientoId identificador opcional del estacionamiento usado como filtro
     * @param jwt JWT validado por Spring Security con el claim usuarioId del usuario autenticado
     * @param httpRequest solicitud HTTP usada para construir ApiResponse con transactionId
     * @return respuesta estandarizada con la pagina de tickets
     */
    @Operation(
            summary = "Listar tickets",
            description = """
                    Consulta tickets activos de forma paginada.
                    ADMIN ve todos los tickets, OWNER ve tickets de sus estacionamientos,
                    OPERADOR ve tickets de estacionamientos asignados y USER ve sus propios tickets.
                    Permite filtrar opcionalmente por estado y estacionamientoId.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tickets consultados correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticacion requerida o token invalido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permiso para consultar tickets"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'OPERADOR', 'USER')")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> getTickets(
            @ParameterObject Pageable pageable,
            @Parameter(description = "Estado opcional del ticket", example = "ABIERTO")
            @RequestParam(required = false) com.kasaca.parkio.ticket.entity.EstadoTicket estado,
            @Parameter(description = "Identificador opcional del estacionamiento", example = "1")
            @RequestParam(required = false) Long estacionamientoId,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info(
                "INICIO - Listado de tickets. estado={}, estacionamientoId={}",
                estado,
                estacionamientoId
        );

        Long usuarioAutenticadoId = getUsuarioId(jwt);
        PageResponse<TicketResponse> response = ticketService.getTickets(
                usuarioAutenticadoId,
                estado,
                estacionamientoId,
                pageable
        );

        log.info("FIN - Listado de tickets");

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Tickets consultados correctamente",
                        response
                )
        );
    }

    /**
     * Consulta un ticket activo por identificador validando permisos de alcance.
     *
     * <p>ADMIN puede consultar cualquier ticket. OWNER solo tickets de sus estacionamientos.
     * OPERADOR solo tickets de estacionamientos asignados y USER solo tickets propios.</p>
     *
     * @param ticketId identificador interno del ticket solicitado
     * @param jwt JWT validado por Spring Security con el claim usuarioId del usuario autenticado
     * @param httpRequest solicitud HTTP usada para construir ApiResponse con transactionId
     * @return respuesta estandarizada con el ticket consultado
     */
    @Operation(
            summary = "Consultar ticket por ID",
            description = """
                    Consulta un ticket activo por identificador.
                    ADMIN puede consultar cualquier ticket, OWNER solo tickets de sus estacionamientos,
                    OPERADOR solo tickets de estacionamientos asignados y USER solo tickets propios.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Ticket consultado correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticacion requerida o token invalido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permiso para consultar tickets"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Usuario autenticado o ticket no encontrado"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "El usuario autenticado no tiene alcance sobre el ticket"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'OPERADOR', 'USER')")
    @GetMapping(
            value = "/{ticketId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(
            @Parameter(description = "Identificador interno del ticket", example = "1")
            @PathVariable Long ticketId,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info("INICIO - Consulta de ticket. ticketId={}", ticketId);

        Long usuarioAutenticadoId = getUsuarioId(jwt);
        TicketResponse response = ticketService.getTicketById(usuarioAutenticadoId, ticketId);

        log.info("FIN - Consulta de ticket. ticketId={}", response.id());

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Ticket consultado correctamente",
                        response
                )
        );
    }

    /**
     * Registra la entrada de un vehiculo al estacionamiento usando un codigo de reserva vigente.
     *
     * <p>Este endpoint lo usa el operador cuando el cliente llega al estacionamiento
     * y presenta su codigo de reserva. Si la reserva es valida, el backend crea un ticket,
     * marca la reserva como USADA y cambia el cajon a OCUPADO.</p>
     *
     * @param request contiene el codigo publico de la reserva que presenta el cliente
     * @param jwt JWT validado por Spring Security con el claim usuarioId del usuario autenticado
     * @param httpRequest solicitud HTTP usada para construir ApiResponse con transactionId
     * @return respuesta estandarizada con el ticket creado
     */
    @Operation(
            summary = "Registrar entrada con reserva",
            description = """
                    Convierte una reserva vigente en un ticket abierto.
                    La reserva debe existir, estar en estado CREADA, no estar vencida
                    ADMIN puede operar cualquier estacionamiento, OWNER solo los propios
                    y OPERADOR solo los estacionamientos asignados.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Ticket creado correctamente"
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
                    description = "El usuario autenticado no tiene permiso para registrar entradas"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Usuario autenticado o reserva no encontrada"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "La reserva no puede convertirse en ticket por su estado actual"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'OPERADOR')")
    @PostMapping(
            value = "/entrada",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<TicketResponse>> registrarEntrada(
            @Valid @RequestBody TicketEntradaRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info("INICIO - Registro de entrada con reserva");

        Long usuarioAutenticadoId = getUsuarioId(jwt);
        TicketResponse response = ticketService.registrarEntrada(usuarioAutenticadoId, request);

        log.info("FIN - Registro de entrada con reserva. ticketId={}", response.id());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.of(
                                httpRequest,
                                HttpStatus.CREATED.value(),
                                "Ticket creado correctamente",
                                response
                        )
                );
    }

    /**
     * Registra la salida de un vehiculo cerrando un ticket abierto.
     *
     * <p>Este endpoint lo usa ADMIN, OWNER u OPERADOR cuando el vehiculo sale
     * del estacionamiento. Si el ticket esta ABIERTO y el usuario autenticado
     * tiene alcance sobre el estacionamiento, el backend calcula el cobro,
     * asigna fecha de salida y deja el ticket en PENDIENTE_PAGO. El cajon
     * se liberara hasta que el pago sea registrado.</p>
     *
     * @param ticketId identificador interno del ticket que se desea cerrar
     * @param jwt JWT validado por Spring Security con el claim usuarioId del usuario autenticado
     * @param httpRequest solicitud HTTP usada para construir ApiResponse con transactionId
     * @return respuesta estandarizada con el ticket cerrado
     */
    @Operation(
            summary = "Registrar salida",
            description = """
                    Cierra un ticket abierto cuando el vehiculo sale del estacionamiento.
                    ADMIN puede operar cualquier estacionamiento, OWNER solo los propios
                    y OPERADOR solo los estacionamientos asignados.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Salida registrada correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticacion requerida o token invalido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permiso para registrar salidas"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Usuario autenticado o ticket no encontrado"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "El ticket no puede cerrarse por su estado o alcance actual"
            )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'OPERADOR')")
    @PatchMapping(
            value = "/{ticketId}/salida",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<TicketResponse>> registrarSalida(
            @Parameter(description = "Identificador interno del ticket", example = "1")
            @PathVariable Long ticketId,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info("INICIO - Registro de salida. ticketId={}", ticketId);

        Long usuarioAutenticadoId = getUsuarioId(jwt);
        TicketResponse response = ticketService.registrarSalida(usuarioAutenticadoId, ticketId);

        log.info("FIN - Registro de salida. ticketId={}", response.id());

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Salida registrada correctamente",
                        response
                )
        );
    }

    /**
     * Obtiene el identificador del usuario autenticado desde el JWT.
     *
     * <p>En este caso el usuario autenticado puede representar a ADMIN, OWNER u
     * OPERADOR. No se recibe desde el frontend para evitar que alguien intente
     * operar tickets a nombre de otro usuario.</p>
     */
    private Long getUsuarioId(Jwt jwt) {
        Number usuarioId = jwt.getClaim("usuarioId");
        return usuarioId.longValue();
    }
}
