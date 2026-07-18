package com.kasaca.parkio.estacionamiento.controller;

import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;
import com.kasaca.parkio.estacionamiento.service.EstacionamientoService;
import com.kasaca.parkio.shared.dto.ApiResponse;
import com.kasaca.parkio.shared.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@RestController
@RequestMapping("/api/estacionamientos")
@RequiredArgsConstructor
@Slf4j
public class EstacionamientoController {

    private final EstacionamientoService estacionamientoService;

    /**
     * Lista los estacionamientos activos de forma paginada para usuarios
     * autenticados con rol ADMIN, OPERADOR o USER.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'USER')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EstacionamientoResponse>>> getEstacionamientos(
            Pageable pageable,
            HttpServletRequest request
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
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'USER')")
    @GetMapping("/{estacionamientoId}")
    public ResponseEntity<ApiResponse<EstacionamientoResponse>> getEstacionamientoById(
            @PathVariable Long estacionamientoId,
            HttpServletRequest request
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
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<EstacionamientoResponse>> addEstacionamiento(
            @Valid @RequestBody EstacionamientoRequest request,
            HttpServletRequest httpRequest
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
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{estacionamientoId}")
    public ResponseEntity<ApiResponse<EstacionamientoResponse>> updateEstacionamiento(
            @PathVariable Long estacionamientoId,
            @Valid @RequestBody EstacionamientoRequest request,
            HttpServletRequest httpRequest
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
     * Elimina lógicamente un estacionamiento activo. La capa de servicio también
     * desactiva sus cajones activos asociados.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{estacionamientoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEstacionamiento(@PathVariable Long estacionamientoId) {
        estacionamientoService.deleteEstacionamiento(estacionamientoId);
    }
}
