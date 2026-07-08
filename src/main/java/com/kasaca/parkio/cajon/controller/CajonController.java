package com.kasaca.parkio.cajon.controller;

import com.kasaca.parkio.cajon.dto.CajonEstadoRequest;
import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.service.CajonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cajones")
@RequiredArgsConstructor
public class CajonController {

    private final CajonService cajonService;

    /**
     * Lista todos los cajones o los filtra por estacionamiento cuando se recibe estacionamientoId.
     *
     * @param estacionamientoId identificador opcional del estacionamiento usado para filtrar cajones
     * @return lista de cajones registrados o pertenecientes al estacionamiento solicitado
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'USER')")
    @GetMapping
    public List<CajonResponse> getCajones(@RequestParam(required = false) Long estacionamientoId) {
        if (estacionamientoId == null) {
            return cajonService.getCajones();
        }

        return cajonService.getCajonesByEstacionamientoId(estacionamientoId);
    }

    /**
     * Consulta un cajón por su identificador.
     *
     * @param cajonId identificador del cajón
     * @return datos del cajón encontrado
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'USER')")
    @GetMapping("/{cajonId}")
    public CajonResponse getCajon(@PathVariable Long cajonId) {
        return cajonService.getCajon(cajonId);
    }

    /**
     * Crea un cajón dentro de un estacionamiento existente.
     *
     * @param request datos necesarios para crear el cajón
     * @return cajón creado con estado HTTP 201
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CajonResponse> addCajon(@Valid @RequestBody CajonRequest request) {
        CajonResponse response = cajonService.addCajon(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Actualiza los datos principales de un cajón existente.
     *
     * @param cajonId identificador del cajón
     * @param request datos actualizados del cajón
     * @return cajón actualizado
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{cajonId}")
    public CajonResponse updateCajon(@PathVariable Long cajonId,@Valid @RequestBody CajonRequest request) {
        return cajonService.updateCajon(cajonId, request);
    }

    /**
     * Cambia únicamente el estado operativo de un cajón.
     *
     * @param cajonId identificador del cajón
     * @param request nuevo estado que se asignará al cajón
     * @return cajón con el estado actualizado
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @PatchMapping("/{cajonId}/estado")
    public CajonResponse updateEstado(@PathVariable Long cajonId, @Valid @RequestBody CajonEstadoRequest request) {
        return cajonService.updateEstado(cajonId, request);
    }

    /**
     * Elimina un cajón mediante su identificador.
     *
     * @param cajonId identificador del cajón
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{cajonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCajon(@PathVariable Long cajonId) {
        cajonService.deleteCajon(cajonId);
    }
}
