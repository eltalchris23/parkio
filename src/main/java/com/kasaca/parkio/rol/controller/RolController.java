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
@RequestMapping("/roles")
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
     * Consulta un rol activo por su identificador y devuelve una respuesta estandarizada.
     *
     * @param rolId identificador del rol consultado
     * @param request solicitud HTTP usada para obtener o generar el transactionId
     * @return respuesta estandarizada con los datos del rol encontrado
     */
    @GetMapping("/{rolId}")
    public ResponseEntity<ApiResponse<RolResponse>> getRol(
            @PathVariable Long rolId,
            HttpServletRequest request
    ) {
        RolResponse response = rolService.getRol(rolId);

        return ResponseEntity.ok(
                ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Rol consultado correctamente",
                        response
                )
        );
    }

    /**
     * Crea un nuevo rol validando los datos recibidos y devuelve una respuesta estandarizada.
     *
     * @param request datos necesarios para crear el rol
     * @param httpRequest solicitud HTTP usada para obtener o generar el transactionId
     * @return respuesta estandarizada con el rol creado y estado HTTP 201
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RolResponse>> addRol(
            @Valid @RequestBody RolRequest request,
            HttpServletRequest httpRequest
    ) {
        RolResponse response = rolService.addRol(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.of(
                                httpRequest,
                                HttpStatus.CREATED.value(),
                                "Rol creado correctamente",
                                response
                        )
                );
    }

    /**
     * Actualiza un rol activo existente usando el identificador de la ruta y los
     * datos validados del cuerpo de la solicitud, devolviendo una respuesta estandarizada.
     *
     * @param rolId identificador del rol que se actualizara
     * @param request datos actualizados del rol
     * @param httpRequest solicitud HTTP usada para obtener o generar el transactionId
     * @return respuesta estandarizada con el rol actualizado
     */
    @PutMapping("/{rolId}")
    public ResponseEntity<ApiResponse<RolResponse>> updateRol(
            @PathVariable Long rolId,
            @Valid @RequestBody RolRequest request,
            HttpServletRequest httpRequest
    ) {
        RolResponse response = rolService.updateRol(rolId, request);

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Rol actualizado correctamente",
                        response
                )
        );
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
