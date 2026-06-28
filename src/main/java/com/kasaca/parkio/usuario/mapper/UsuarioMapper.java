package com.kasaca.parkio.usuario.mapper;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.rol.entity.Rol;
import com.kasaca.parkio.usuario.dto.UsuarioCreateRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.dto.UsuarioUpdateRequest;
import com.kasaca.parkio.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UsuarioMapper {

    /**
     * Crea una entidad Usuario nueva a partir de los datos de entrada y de una
     * contraseña previamente procesada por el servicio.
     *
     * @param request datos recibidos para crear al usuario
     * @param passwordHash hash seguro de la contraseña; nunca debe ser texto plano
     * @return una entidad Usuario lista para persistirse
     */
    public Usuario toEntity(UsuarioCreateRequest request, String passwordHash) {
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordHash);
        return usuario;
    }

    /**
     * Actualiza los datos editables de una entidad Usuario existente. No modifica
     * sus roles, estacionamientos, identificador ni campos de auditoría.
     *
     * @param request datos con los que se actualizará la entidad
     * @param usuario entidad que recibirá los cambios
     */
    public void updateEntity(UsuarioUpdateRequest request, Usuario usuario) {
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email());
    }

    /**
     * Convierte una entidad Usuario en el DTO público de respuesta. Expone los
     * nombres de sus roles, pero omite la contraseña y las relaciones JPA completas.
     *
     * @param usuario entidad que se convertirá en respuesta
     * @return DTO con los datos públicos del usuario
     */
    public UsuarioResponse toResponse(Usuario usuario) {
        Set<String> roles = usuario.getRoles()
                .stream()
                .map(Rol::getNombre)
                .collect(Collectors.toSet());

        Set<Long> estacionamientoIds = usuario.getEstacionamientos()
                .stream()
                .map(Estacionamiento::getId)
                .collect(Collectors.toSet());

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getActivo(),
                usuario.getFechaCreacion(),
                roles,
                estacionamientoIds
        );
    }
}
