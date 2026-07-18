package com.kasaca.parkio.cajon.controller;

import com.kasaca.parkio.cajon.dto.CajonEstadoRequest;
import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.service.CajonService;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
public class CajonController {

    private final CajonService cajonService;

    /**
     * Lista cajones activos de forma paginada o los filtra por estacionamiento
     * cuando se recibe estacionamientoId.
     *
     * @param estacionamientoId identificador opcional del estacionamiento usado para filtrar cajones
     * @return respuesta estandarizada con cajones paginados
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'USER')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CajonResponse>>> getCajones(
            @RequestParam(required = false) Long estacionamientoId,
            Pageable pageable,
            HttpServletRequest request
    ) {
        log.info("INICIO - Listado de cajones");
        PageResponse<CajonResponse> cajones;

        if (estacionamientoId == null) {
            cajones = cajonService.getCajones(pageable);
        } else {
            cajones = cajonService.getCajonesByEstacionamientoId(
                    estacionamientoId,
                    pageable
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
     * Consulta un cajón activo por su identificador.
     *
     * @param cajonId identificador del cajón
     * @param request solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con los datos del cajón encontrado
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'USER')")
    @GetMapping("/{cajonId}")
    public ResponseEntity<ApiResponse<CajonResponse>> getCajon(
            @PathVariable Long cajonId,
            HttpServletRequest request
    ) {
        CajonResponse response = cajonService.getCajon(cajonId);

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
     * @param request datos necesarios para crear el cajón
     * @param httpRequest solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con el cajón creado y estado HTTP 201
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<CajonResponse>> addCajon(
            @Valid @RequestBody CajonRequest request,
            HttpServletRequest httpRequest
    ) {
        CajonResponse response = cajonService.addCajon(request);

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
     * @param cajonId identificador del cajón
     * @param request datos actualizados del cajón
     * @param httpRequest solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con el cajón actualizado
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{cajonId}")
    public ResponseEntity<ApiResponse<CajonResponse>> updateCajon(
            @PathVariable Long cajonId,
            @Valid @RequestBody CajonRequest request,
            HttpServletRequest httpRequest
    ) {
        CajonResponse response = cajonService.updateCajon(cajonId, request);

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
     * @param cajonId identificador del cajón
     * @param request nuevo estado que se asignará al cajón
     * @param httpRequest solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con el cajón con estado actualizado
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @PatchMapping("/{cajonId}/estado")
    public ResponseEntity<ApiResponse<CajonResponse>> updateEstado(
            @PathVariable Long cajonId,
            @Valid @RequestBody CajonEstadoRequest request,
            HttpServletRequest httpRequest
    ) {
        CajonResponse response = cajonService.updateEstado(cajonId, request);

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
