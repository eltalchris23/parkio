package com.kasaca.parkio.usuario.controller;

import com.kasaca.parkio.shared.dto.ApiResponse;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.usuario.dto.UsuarioCreateRequest;
import com.kasaca.parkio.usuario.dto.UsuarioEstacionamientoRequest;
import com.kasaca.parkio.usuario.dto.UsuarioPasswordRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.dto.UsuarioRolRequest;
import com.kasaca.parkio.usuario.dto.UsuarioUpdateRequest;
import com.kasaca.parkio.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Tag(
        name = "Usuarios",
        description = "Endpoints para registrar, consultar y administrar usuarios del sistema"
)
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Consulta los usuarios activos de forma paginada.
     *
     * @param pageable parámetros de paginación y ordenamiento recibidos por query params
     * @param request solicitud HTTP usada para construir la respuesta estandarizada con transactionId
     * @return respuesta estandarizada con la página de usuarios y metadatos de paginación
     */
    @Operation(
            summary = "Listar usuarios activos",
            description = """
                    Devuelve usuarios activos de forma paginada.
                    Acepta page, size y sort como parámetros de consulta.
                    Requiere rol ADMIN.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Usuarios consultados correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para consultar usuarios"
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<UsuarioResponse>>> getAllUsers(
            @ParameterObject Pageable pageable,

            @Parameter(hidden = true)
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
     * @return respuesta estandarizada con los datos públicos del usuario encontrado
     */
    @Operation(
            summary = "Consultar usuario por identificador",
            description = """
                    Devuelve la información pública de un usuario activo.
                    ADMIN puede consultar cualquier usuario.
                    USER y OPERADOR solo pueden consultar su propio usuario.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Usuario consultado correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para consultar este usuario"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado"
            )
    })
    @PreAuthorize("hasRole('ADMIN') or (hasAnyRole('USER', 'OPERADOR') and @usuarioSecurity.isSelf(authentication, #usuarioId))")
    @GetMapping(value = "/{usuarioId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> getUserById(
            @Parameter(description = "Identificador del usuario", example = "1")
            @PathVariable Long usuarioId,

            @Parameter(hidden = true)
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
    @Operation(
            summary = "Registrar usuario",
            description = """
                    Crea un usuario nuevo usando nombre, apellido, email y contraseña.
                    Este endpoint es público y asigna automáticamente el rol base USER.
                    La contraseña se almacena como hash BCrypt, nunca en texto plano.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Usuario creado correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos en la solicitud"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Ya existe un usuario con el mismo correo electrónico"
            )
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<UsuarioResponse>> addUser(
            @Valid @RequestBody UsuarioCreateRequest request,

            @Parameter(hidden = true)
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
     * Actualiza los datos generales de un usuario existente.
     *
     * @param usuarioId identificador del usuario
     * @param request datos actualizados
     * @param httpRequest solicitud HTTP usada para obtener el transactionId
     * @return respuesta estandarizada con el usuario actualizado
     */
    @Operation(
            summary = "Actualizar usuario",
            description = """
                    Actualiza nombre, apellido y correo electrónico de un usuario.
                    No modifica la contraseña ni las asociaciones de roles o estacionamientos.
                    ADMIN puede actualizar cualquier usuario.
                    USER y OPERADOR solo pueden actualizar su propio usuario.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Usuario actualizado correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos en la solicitud"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para actualizar este usuario"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Ya existe otro usuario con el mismo correo electrónico"
            )
    })
    @PreAuthorize("hasRole('ADMIN') or (hasAnyRole('USER', 'OPERADOR') and @usuarioSecurity.isSelf(authentication, #usuarioId))")
    @PutMapping(
            value = "/{usuarioId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<UsuarioResponse>> updateUser(
            @Parameter(description = "Identificador del usuario", example = "1")
            @PathVariable Long usuarioId,

            @Valid @RequestBody UsuarioUpdateRequest request,

            @Parameter(hidden = true)
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
    @Operation(
            summary = "Cambiar contraseña de usuario",
            description = """
                    Reemplaza únicamente la contraseña de un usuario.
                    La nueva contraseña se almacena como hash BCrypt.
                    ADMIN puede cambiar la contraseña de cualquier usuario.
                    USER y OPERADOR solo pueden cambiar su propia contraseña.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Contraseña actualizada correctamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos en la solicitud"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para cambiar esta contraseña"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado"
            )
    })
    @PreAuthorize("hasRole('ADMIN') or (hasAnyRole('USER', 'OPERADOR') and @usuarioSecurity.isSelf(authentication, #usuarioId))")
    @PatchMapping(
            value = "/{usuarioId}/password",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(
            @Parameter(description = "Identificador del usuario", example = "1")
            @PathVariable Long usuarioId,

            @Valid @RequestBody UsuarioPasswordRequest request
    ) {
        usuarioService.updatePassword(usuarioId, request);
    }

    /**
     * Elimina lógicamente un usuario mediante su identificador.
     *
     * @param usuarioId identificador del usuario
     */
    @Operation(
            summary = "Eliminar usuario",
            description = """
                    Desactiva lógicamente un usuario cambiando su campo activo a false.
                    No elimina físicamente el registro de la base de datos.
                    Un usuario inactivo no puede iniciar sesión.
                    Requiere rol ADMIN.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Usuario eliminado lógicamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para eliminar usuarios"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado"
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @Parameter(description = "Identificador del usuario", example = "1")
            @PathVariable Long usuarioId
    ) {
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
    @Operation(
            summary = "Asignar rol a usuario",
            description = """
                    Asigna un rol existente a un usuario activo.
                    Requiere rol ADMIN.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rol asignado correctamente al usuario"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos en la solicitud"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para asignar roles"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Usuario o rol no encontrado"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "El usuario ya tiene asignado el rol"
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(
            value = "/{usuarioId}/roles",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<UsuarioResponse>> assignRole(
            @Parameter(description = "Identificador del usuario", example = "1")
            @PathVariable Long usuarioId,

            @Valid @RequestBody UsuarioRolRequest request,

            @Parameter(hidden = true)
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
    @Operation(
            summary = "Retirar rol de usuario",
            description = """
                    Retira un rol previamente asignado a un usuario.
                    Requiere rol ADMIN.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Rol retirado correctamente del usuario"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para retirar roles"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Usuario, rol o asignación no encontrada"
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{usuarioId}/roles/{rolId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRole(
            @Parameter(description = "Identificador del usuario", example = "1")
            @PathVariable Long usuarioId,

            @Parameter(description = "Identificador del rol", example = "2")
            @PathVariable Long rolId
    ) {
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
    @Operation(
            summary = "Asignar estacionamiento a usuario",
            description = """
                    Asigna un estacionamiento existente a un usuario activo.
                    Requiere rol ADMIN.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Estacionamiento asignado correctamente al usuario"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos en la solicitud"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para asignar estacionamientos"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Usuario o estacionamiento no encontrado"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "El usuario ya tiene asignado el estacionamiento"
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(
            value = "/{usuarioId}/estacionamientos",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<UsuarioResponse>> assignEstacionamiento(
            @Parameter(description = "Identificador del usuario", example = "1")
            @PathVariable Long usuarioId,

            @Valid @RequestBody UsuarioEstacionamientoRequest request,

            @Parameter(hidden = true)
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
    @Operation(
            summary = "Retirar estacionamiento de usuario",
            description = """
                    Retira un estacionamiento previamente asignado a un usuario.
                    Requiere rol ADMIN.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Estacionamiento retirado correctamente del usuario"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para retirar estacionamientos"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Usuario, estacionamiento o asignación no encontrada"
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{usuarioId}/estacionamientos/{estacionamientoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEstacionamiento(
            @Parameter(description = "Identificador del usuario", example = "1")
            @PathVariable Long usuarioId,

            @Parameter(description = "Identificador del estacionamiento", example = "1")
            @PathVariable Long estacionamientoId
    ) {
        usuarioService.removeEstacionamiento(usuarioId, estacionamientoId);
    }
}
