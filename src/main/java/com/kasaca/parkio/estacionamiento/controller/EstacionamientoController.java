package com.kasaca.parkio.estacionamiento.controller;

import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;
import com.kasaca.parkio.estacionamiento.service.EstacionamientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping("/api/estacionamientos")
@RequiredArgsConstructor
public class EstacionamientoController {

    private final EstacionamientoService estacionamientoService;

    /**
     * Lista los estacionamientos activos disponibles para usuarios autenticados
     * con rol ADMIN, OPERADOR o USER.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'USER')")
    @GetMapping
    public List<EstacionamientoResponse> getEstacionamientos() {
        return estacionamientoService.getEstacionamientos();
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
