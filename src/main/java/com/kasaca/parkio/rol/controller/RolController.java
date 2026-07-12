package com.kasaca.parkio.rol.controller;

import com.kasaca.parkio.rol.dto.RolRequest;
import com.kasaca.parkio.rol.dto.RolResponse;
import com.kasaca.parkio.rol.service.RolService;
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
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class RolController {

    private final RolService rolService;

    /**
     * Lista los roles activos registrados en el sistema usando paginaciÃ³n y
     * ordenamiento recibidos por query params como page, size y sort.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RolResponse>>> getRoles(
            Pageable pageable,
            HttpServletRequest request
    ) {
        log.info("getRoles - Controller");
        PageResponse<RolResponse> roles = rolService.getRoles(pageable);

        return ResponseEntity.ok(
                ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Roles consultados correctamente",
                        roles
                )
        );
    }

    /**
     * Consulta un rol activo por su identificador.
     */
    @GetMapping("/{rolId}")
    public RolResponse getRol(@PathVariable Long rolId) {
        return rolService.getRol(rolId);
    }

    /**
     * Crea un nuevo rol validando los datos recibidos en la solicitud.
     */
    @PostMapping
    public ResponseEntity<RolResponse> addRol(
            @Valid @RequestBody RolRequest request
    ) {
        RolResponse response = rolService.addRol(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Actualiza un rol activo existente usando el identificador de la ruta y los
     * datos validados del cuerpo de la solicitud.
     */
    @PutMapping("/{rolId}")
    public RolResponse updateRol(
            @PathVariable Long rolId,
            @Valid @RequestBody RolRequest request
    ) {
        return rolService.updateRol(rolId, request);
    }

    /**
     * Elimina lógicamente un rol activo cambiando su estado a inactivo desde la
     * capa de servicio.
     */
    @DeleteMapping("/{rolId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRol(@PathVariable Long rolId) {
        rolService.deleteRol(rolId);
    }
}
