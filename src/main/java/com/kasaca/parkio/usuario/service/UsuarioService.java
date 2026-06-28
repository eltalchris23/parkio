package com.kasaca.parkio.usuario.service;

import com.kasaca.parkio.usuario.dto.UsuarioRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.dto.UsuarioEstacionamientoRequest;
import com.kasaca.parkio.usuario.dto.UsuarioRolRequest;

import java.util.List;

public interface UsuarioService {

    List<UsuarioResponse> getAllUsers();

    UsuarioResponse getUserById(Long id);

    UsuarioResponse addUser(UsuarioRequest request);

    UsuarioResponse updateUser(Long id, UsuarioRequest request);

    void deleteUser(Long id);

    /**
     * Asigna un rol existente a un usuario.
     *
     * @param usuarioId identificador del usuario que recibirá el rol
     * @param request solicitud que contiene el identificador del rol
     * @return usuario con su lista de roles actualizada
     */
    UsuarioResponse assignRole(Long usuarioId, UsuarioRolRequest request);

    /**
     * Retira de un usuario un rol previamente asignado.
     *
     * @param usuarioId identificador del usuario
     * @param rolId identificador del rol que se retirará
     */
    void removeRole(Long usuarioId, Long rolId);

    /**
     * Asigna un estacionamiento existente a un usuario.
     *
     * @param usuarioId identificador del usuario
     * @param request solicitud que contiene el identificador del estacionamiento
     * @return usuario con sus estacionamientos actualizados
     */
    UsuarioResponse assignEstacionamiento(Long usuarioId, UsuarioEstacionamientoRequest request);

    /**
     * Retira de un usuario un estacionamiento previamente asignado.
     *
     * @param usuarioId identificador del usuario
     * @param estacionamientoId identificador del estacionamiento que se retirará
     */
    void removeEstacionamiento(Long usuarioId, Long estacionamientoId);
}
