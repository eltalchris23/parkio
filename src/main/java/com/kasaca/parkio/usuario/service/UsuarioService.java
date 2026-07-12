package com.kasaca.parkio.usuario.service;

import com.kasaca.parkio.usuario.dto.UsuarioCreateRequest;
import com.kasaca.parkio.usuario.dto.UsuarioPasswordRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.dto.UsuarioEstacionamientoRequest;
import com.kasaca.parkio.usuario.dto.UsuarioRolRequest;
import com.kasaca.parkio.usuario.dto.UsuarioUpdateRequest;
import com.kasaca.parkio.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    /**
     * Consulta usuarios activos usando paginacion y ordenamiento.
     *
     * @param pageable parametros de paginacion y ordenamiento
     * @return pagina estandarizada con usuarios activos
     */
    PageResponse<UsuarioResponse> getAllUsers(Pageable pageable);

    /**
     * Consulta un usuario activo por su identificador.
     *
     * @param id identificador del usuario
     * @return datos publicos del usuario encontrado
     */
    UsuarioResponse getUserById(Long id);

    /**
     * Crea un usuario nuevo, genera el hash de su password y asigna el rol base correspondiente.
     *
     * @param request datos necesarios para crear el usuario
     * @return usuario creado sin exponer informacion sensible
     */
    UsuarioResponse addUser(UsuarioCreateRequest request);

    /**
     * Actualiza los datos generales de un usuario sin modificar su password ni sus relaciones.
     *
     * @param id identificador del usuario
     * @param request datos generales actualizados
     * @return usuario actualizado
     */
    UsuarioResponse updateUser(Long id, UsuarioUpdateRequest request);

    /**
     * Reemplaza la contraseña de un usuario utilizando un hash seguro.
     *
     * @param id identificador del usuario
     * @param request solicitud que contiene la nueva contraseña
     */
    void updatePassword(Long id, UsuarioPasswordRequest request);

    /**
     * Desactiva logicamente un usuario para conservar su registro por auditoria.
     *
     * @param id identificador del usuario
     */
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
