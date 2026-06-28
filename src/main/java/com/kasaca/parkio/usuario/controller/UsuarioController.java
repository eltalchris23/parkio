package com.kasaca.parkio.usuario.controller;

import com.kasaca.parkio.usuario.dto.UsuarioCreateRequest;
import com.kasaca.parkio.usuario.dto.UsuarioPasswordRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.dto.UsuarioEstacionamientoRequest;
import com.kasaca.parkio.usuario.dto.UsuarioRolRequest;
import com.kasaca.parkio.usuario.dto.UsuarioUpdateRequest;
import com.kasaca.parkio.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Obtiene todos los usuarios registrados.
     *
     * @return lista con los datos públicos de los usuarios
     */
    @GetMapping
    public List<UsuarioResponse> getAllUsers() {
        return usuarioService.getAllUsers();
    }

    /**
     * Obtiene un usuario mediante su identificador.
     *
     * @param usuarioId identificador del usuario
     * @return datos públicos del usuario encontrado
     */
    @GetMapping("/{usuarioId}")
    public UsuarioResponse getUserById(@PathVariable Long usuarioId) {
        return usuarioService.getUserById(usuarioId);
    }

    /**
     * Crea un usuario validando los datos recibidos.
     *
     * @param request datos del usuario nuevo
     * @return usuario creado con estado HTTP 201
     */
    @PostMapping
    public ResponseEntity<UsuarioResponse> addUser(@Valid @RequestBody UsuarioCreateRequest request) {
        UsuarioResponse response = usuarioService.addUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Actualiza los datos de un usuario existente.
     *
     * @param usuarioId identificador del usuario
     * @param request datos actualizados
     * @return usuario actualizado
     */
    @PutMapping("/{usuarioId}")
    public UsuarioResponse updateUser(@PathVariable Long usuarioId, @Valid @RequestBody UsuarioUpdateRequest request) {
        return usuarioService.updateUser(usuarioId, request);
    }

    /**
     * Reemplaza la contraseña de un usuario sin modificar sus datos generales.
     *
     * @param usuarioId identificador del usuario
     * @param request solicitud que contiene la nueva contraseña
     */
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
     * @return usuario con su lista de roles actualizada
     */
    @PostMapping("/{usuarioId}/roles")
    public UsuarioResponse assignRole(@PathVariable Long usuarioId, @Valid @RequestBody UsuarioRolRequest request) {
        return usuarioService.assignRole(usuarioId, request);
    }

    /**
     * Retira un rol previamente asignado a un usuario.
     *
     * @param usuarioId identificador del usuario
     * @param rolId identificador del rol que se retirará
     */
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
     * @return usuario con sus estacionamientos actualizados
     */
    @PostMapping("/{usuarioId}/estacionamientos")
    public UsuarioResponse assignEstacionamiento(@PathVariable Long usuarioId, @Valid @RequestBody UsuarioEstacionamientoRequest request) {
        return usuarioService.assignEstacionamiento(usuarioId, request);
    }

    /**
     * Retira un estacionamiento previamente asignado a un usuario.
     *
     * @param usuarioId identificador del usuario
     * @param estacionamientoId identificador del estacionamiento
     */
    @DeleteMapping("/{usuarioId}/estacionamientos/{estacionamientoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEstacionamiento(@PathVariable Long usuarioId, @PathVariable Long estacionamientoId) {
        usuarioService.removeEstacionamiento(usuarioId, estacionamientoId);
    }
}
