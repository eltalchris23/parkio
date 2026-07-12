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
     * autenticados con rol ADMIN, OPERADOR o USER.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'USER')")
    @GetMapping("/{estacionamientoId}")
    public EstacionamientoResponse getEstacionamientoById(@PathVariable Long estacionamientoId) {
        return estacionamientoService.getEstacionamientoById(estacionamientoId);
    }

    /**
     * Crea un nuevo estacionamiento con datos validados. Esta operación solo está
     * permitida para usuarios con rol ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EstacionamientoResponse> addEstacionamiento(@Valid @RequestBody EstacionamientoRequest request) {
        EstacionamientoResponse response = estacionamientoService.addEstacionamiento(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Actualiza un estacionamiento activo existente. Esta operación solo está
     * permitida para usuarios con rol ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{estacionamientoId}")
    public EstacionamientoResponse updateEstacionamiento(@PathVariable Long estacionamientoId,@Valid @RequestBody EstacionamientoRequest request) {
        return estacionamientoService.updateEstacionamiento(estacionamientoId, request);
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
