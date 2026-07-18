package com.kasaca.parkio.usuario.controller;

import com.kasaca.parkio.usuario.dto.UsuarioCreateRequest;
import com.kasaca.parkio.usuario.dto.UsuarioPasswordRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.dto.UsuarioEstacionamientoRequest;
import com.kasaca.parkio.usuario.dto.UsuarioRolRequest;
import com.kasaca.parkio.usuario.dto.UsuarioUpdateRequest;
import com.kasaca.parkio.usuario.service.UsuarioService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Slf4j
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Consulta los usuarios activos de forma paginada.
     *
     * @param pageable parametros de paginacion y ordenamiento recibidos por query params
     * @param request solicitud HTTP usada para construir la respuesta estandarizada con transactionId
     * @return respuesta estandarizada con la pagina de usuarios y metadatos de paginacion
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UsuarioResponse>>> getAllUsers(
            Pageable pageable,
            HttpServletRequest request
    ) {
        log.info("INICIO - lista de usuarios");
        PageResponse<UsuarioResponse> usuarios = usuarioService.getAllUsers(pageable);
        log.info("FINAL - lista de usuarios");

        return ResponseEntity.ok(
                ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Usuarios consultados correctamente",
                        usuarios
                )
        );
    }

    /**
     * Obtiene un usuario activo mediante su identificador.
     *
     * @param usuarioId identificador del usuario
     * @param request solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con los datos publicos del usuario encontrado
     */
    @PreAuthorize("hasRole('ADMIN') or (hasAnyRole('USER', 'OPERADOR') and @usuarioSecurity.isSelf(authentication, #usuarioId))")
    @GetMapping("/{usuarioId}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> getUserById(
            @PathVariable Long usuarioId,
            HttpServletRequest request
    ) {
        UsuarioResponse response = usuarioService.getUserById(usuarioId);

        return ResponseEntity.ok(
                ApiResponse.of(
                        request,
                        HttpStatus.OK.value(),
                        "Usuario consultado correctamente",
                        response
                )
        );
    }

    /**
     * Crea un usuario validando los datos recibidos.
     *
     * @param request datos del usuario nuevo
     * @param httpRequest solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con el usuario creado y estado HTTP 201
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponse>> addUser(
            @Valid @RequestBody UsuarioCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        UsuarioResponse response = usuarioService.addUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.of(
                                httpRequest,
                                HttpStatus.CREATED.value(),
                                "Usuario creado correctamente",
                                response
                        )
                );
    }

    /**
     * Actualiza los datos de un usuario existente.
     *
     * @param usuarioId identificador del usuario
     * @param request datos actualizados
     * @param httpRequest solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con el usuario actualizado
     */
    @PreAuthorize("hasRole('ADMIN') or (hasAnyRole('USER', 'OPERADOR') and @usuarioSecurity.isSelf(authentication, #usuarioId))")
    @PutMapping("/{usuarioId}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> updateUser(
            @PathVariable Long usuarioId,
            @Valid @RequestBody UsuarioUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        UsuarioResponse response = usuarioService.updateUser(usuarioId, request);

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Usuario actualizado correctamente",
                        response
                )
        );
    }

    /**
     * Reemplaza la contraseña de un usuario sin modificar sus datos generales.
     *
     * @param usuarioId identificador del usuario
     * @param request solicitud que contiene la nueva contraseña
     */
    @PreAuthorize("hasRole('ADMIN') or (hasAnyRole('USER', 'OPERADOR') and @usuarioSecurity.isSelf(authentication, #usuarioId))")
    @PatchMapping("/{usuarioId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(@PathVariable Long usuarioId, @Valid @RequestBody UsuarioPasswordRequest request) {
        usuarioService.updatePassword(usuarioId, request);
    }

    /**
     * Elimina un usuario mediante su identificador.
     *
     * @param usuarioId identificador del usuario
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long usuarioId) {
        usuarioService.deleteUser(usuarioId);
    }

    /**
     * Asigna un rol existente a un usuario.
     *
     * @param usuarioId identificador del usuario que recibirá el rol
     * @param request solicitud que contiene el identificador del rol
     * @param httpRequest solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con el usuario y su lista de roles actualizada
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{usuarioId}/roles")
    public ResponseEntity<ApiResponse<UsuarioResponse>> assignRole(
            @PathVariable Long usuarioId,
            @Valid @RequestBody UsuarioRolRequest request,
            HttpServletRequest httpRequest
    ) {
        UsuarioResponse response = usuarioService.assignRole(usuarioId, request);

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Rol asignado correctamente al usuario",
                        response
                )
        );
    }

    /**
     * Retira un rol previamente asignado a un usuario.
     *
     * @param usuarioId identificador del usuario
     * @param rolId identificador del rol que se retirará
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{usuarioId}/roles/{rolId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRole(@PathVariable Long usuarioId, @PathVariable Long rolId) {
        usuarioService.removeRole(usuarioId, rolId);
    }

    /**
     * Asigna un estacionamiento existente a un usuario.
     *
     * @param usuarioId identificador del usuario
     * @param request solicitud que contiene el identificador del estacionamiento
     * @param httpRequest solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con el usuario y sus estacionamientos actualizados
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{usuarioId}/estacionamientos")
    public ResponseEntity<ApiResponse<UsuarioResponse>> assignEstacionamiento(
            @PathVariable Long usuarioId,
            @Valid @RequestBody UsuarioEstacionamientoRequest request,
            HttpServletRequest httpRequest
    ) {
        UsuarioResponse response = usuarioService.assignEstacionamiento(usuarioId, request);

        return ResponseEntity.ok(
                ApiResponse.of(
                        httpRequest,
                        HttpStatus.OK.value(),
                        "Estacionamiento asignado correctamente al usuario",
                        response
                )
        );
    }

    /**
     * Retira un estacionamiento previamente asignado a un usuario.
     *
     * @param usuarioId identificador del usuario
     * @param estacionamientoId identificador del estacionamiento
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{usuarioId}/estacionamientos/{estacionamientoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEstacionamiento(@PathVariable Long usuarioId, @PathVariable Long estacionamientoId) {
        usuarioService.removeEstacionamiento(usuarioId, estacionamientoId);
    }
}
