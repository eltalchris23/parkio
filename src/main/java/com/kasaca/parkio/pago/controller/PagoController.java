package com.kasaca.parkio.pago.controller;

import com.kasaca.parkio.pago.dto.PagoRequest;
import com.kasaca.parkio.pago.dto.PagoResponse;
import com.kasaca.parkio.pago.entity.MetodoPago;
import com.kasaca.parkio.pago.service.PagoService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controlador REST del modulo de pagos.
 *
 * <p>Expone operaciones para registrar el cobro de tickets pendientes de pago
 * y consultar el pago asociado a un ticket.</p>
 */
@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pagos", description = "Registro y consulta de pagos de tickets")
@SecurityRequirement(name = "bearerAuth")
public class PagoController {

    private final PagoService pagoService;

    /**
     * Consulta pagos activos de forma paginada y con filtros opcionales.
     *
     * <p>ADMIN ve todos los pagos. OWNER ve pagos de sus estacionamientos.
     * OPERADOR ve pagos de sus estacionamientos asignados. USER no tiene acceso
     * a este listado general y debe usar la consulta por ticket para sus pagos.</p>
     *
     * @param pageable parametros de paginacion y ordenamiento recibidos como page, size y sort
     * @param estacionamientoId identificador opcional del estacionamiento usado como filtro
     * @param metodoPago metodo opcional para filtrar pagos EFECTIVO, TARJETA o TRANSFERENCIA
     * @param fechaInicio fecha inicial opcional del rango de busqueda
     * @param fechaFin fecha final opcional del rango de busqueda
     * @param jwt JWT validado por Spring Security con el claim usuarioId
     * @param httpRequest solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con la pagina de pagos
     */
    @Operation(
            summary = "Listar pagos",
            description = """
                    Consulta pagos activos de forma paginada.
                    ADMIN ve todos los pagos, OWNER ve pagos de sus estacionamientos
                    y OPERADOR ve pagos de estacionamientos asignados.
                    Permite filtrar opcionalmente por estacionamientoId, metodoPago,
                    fechaInicio y fechaFin.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pagos consultados correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida o token invalido"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene permiso para listar pagos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El rango de fechas o el alcance del usuario no es valido")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'OPERADOR')")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<PagoResponse>>> getPagos(
            @ParameterObject Pageable pageable,
            @Parameter(description = "Identificador opcional del estacionamiento", example = "1")
            @RequestParam(required = false) Long estacionamientoId,
            @Parameter(description = "Metodo de pago opcional", example = "EFECTIVO")
            @RequestParam(required = false) MetodoPago metodoPago,
            @Parameter(description = "Fecha inicial opcional en formato ISO yyyy-MM-dd", example = "2026-08-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,
            @Parameter(description = "Fecha final opcional en formato ISO yyyy-MM-dd", example = "2026-08-31")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info(
                "INICIO - Listado de pagos. estacionamientoId={}, metodoPago={}, fechaInicio={}, fechaFin={}",
                estacionamientoId,
                metodoPago,
                fechaInicio,
                fechaFin
        );

        Long usuarioAutenticadoId = getUsuarioId(jwt);
        PageResponse<PagoResponse> response = pagoService.getPagos(
                usuarioAutenticadoId,
                estacionamientoId,
                metodoPago,
                fechaInicio,
                fechaFin,
                pageable
        );

        log.info("FIN - Listado de pagos");

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Pagos consultados correctamente",
                        response
                )
        );
    }

    /**
     * Registra el pago de un ticket en estado PENDIENTE_PAGO.
     *
     * <p>ADMIN puede registrar cualquier pago. OWNER solo pagos de sus
     * estacionamientos. OPERADOR solo pagos de estacionamientos asignados.
     * Al registrar el pago, el ticket queda CERRADO y el cajon se libera.</p>
     *
     * @param request datos del ticket, monto recibido y metodo de pago
     * @param jwt JWT validado por Spring Security con el claim usuarioId
     * @param httpRequest solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con el pago registrado
     */
    @Operation(
            summary = "Registrar pago",
            description = """
                    Registra el pago de un ticket en estado PENDIENTE_PAGO.
                    Calcula el cambio cuando el monto recibido es mayor al monto total.
                    Al confirmar el pago, el ticket pasa a CERRADO y el cajon queda LIBRE.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Pago registrado correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos invalidos en la solicitud"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida o token invalido"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene permiso para registrar pagos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario autenticado o ticket no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El ticket no puede pagarse por su estado, importe o alcance actual")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'OPERADOR')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagoResponse>> registrarPago(
            @Valid @RequestBody PagoRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info("INICIO - Registro de pago. ticketId={}", request.ticketId());

        Long usuarioAutenticadoId = getUsuarioId(jwt);
        PagoResponse response = pagoService.registrarPago(usuarioAutenticadoId, request);

        log.info("FIN - Registro de pago. pagoId={}, ticketId={}", response.id(), response.ticketId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.of(
                                httpRequest,
                                HttpStatus.CREATED.value(),
                                "Pago registrado correctamente",
                                response
                        )
                );
    }

    /**
     * Consulta el pago activo asociado a un ticket.
     *
     * <p>ADMIN consulta cualquier pago. OWNER, OPERADOR y USER solo consultan
     * pagos dentro de su alcance permitido.</p>
     *
     * @param ticketId identificador interno del ticket
     * @param jwt JWT validado por Spring Security con el claim usuarioId
     * @param httpRequest solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con el pago encontrado
     */
    @Operation(
            summary = "Consultar pago por ticket",
            description = "Consulta el pago activo registrado para un ticket respetando el alcance del usuario autenticado."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pago consultado correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida o token invalido"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene permiso para consultar pagos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario autenticado, ticket o pago no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El usuario autenticado no tiene alcance sobre el pago")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'OPERADOR', 'USER')")
    @GetMapping(value = "/ticket/{ticketId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagoResponse>> getPagoByTicketId(
            @Parameter(description = "Identificador interno del ticket", example = "1")
            @PathVariable Long ticketId,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        log.info("INICIO - Consulta de pago por ticket. ticketId={}", ticketId);

        Long usuarioAutenticadoId = getUsuarioId(jwt);
        PagoResponse response = pagoService.getPagoByTicketId(usuarioAutenticadoId, ticketId);

        log.info("FIN - Consulta de pago por ticket. pagoId={}, ticketId={}", response.id(), response.ticketId());

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Pago consultado correctamente",
                        response
                )
        );
    }

    /**
     * Obtiene el identificador del usuario autenticado desde el claim usuarioId del JWT.
     */
    private Long getUsuarioId(Jwt jwt) {
        Number usuarioId = jwt.getClaim("usuarioId");
        return usuarioId.longValue();
    }
}
