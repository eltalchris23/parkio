package com.kasaca.parkio.usuario.service;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.estacionamiento.repository.EstacionamientoRepository;
import com.kasaca.parkio.rol.entity.Rol;
import com.kasaca.parkio.rol.repository.RolRepository;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.usuario.dto.UsuarioCreateRequest;
import com.kasaca.parkio.usuario.dto.UsuarioPasswordRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.dto.UsuarioEstacionamientoRequest;
import com.kasaca.parkio.usuario.dto.UsuarioRolRequest;
import com.kasaca.parkio.usuario.dto.UsuarioUpdateRequest;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.mapper.UsuarioMapper;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    private static final String ROL_USER = "USER";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EstacionamientoRepository estacionamientoRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Obtiene todos los usuarios y los convierte en respuestas públicas.
     */
    @Override
    public List<UsuarioResponse> getAllUsers() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    /**
     * Busca un usuario por su identificador.
     */
    @Override
    public UsuarioResponse getUserById(Long id) {
        return usuarioMapper.toResponse(findUsuarioById(id));
    }

    /**
     * Crea un usuario después de validar el correo, cifrar su contraseña y
     * asignarle automáticamente el rol base USER para que pueda autenticarse con
     * permisos de usuario final.
     */
    @Override
    @Transactional
    public UsuarioResponse addUser(UsuarioCreateRequest request) {
        validateUniqueEmail(request.email());

        String passwordHash = passwordEncoder.encode(request.password());
        Usuario usuario = usuarioMapper.toEntity(request, passwordHash);
        Rol userRole = findRolByNombre(ROL_USER);

        usuario.getRoles().add(userRole);
        Usuario savedUsuario = usuarioRepository.save(usuario);

        return usuarioMapper.toResponse(savedUsuario);
    }

    /**
     * Actualiza los datos generales de un usuario sin modificar su contraseña ni
     * sus asociaciones.
     */
    @Override
    @Transactional
    public UsuarioResponse updateUser(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = findUsuarioById(id);

        if (usuarioRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ConflictException(
                    "Ya existe un usuario con el correo '%s'".formatted(request.email())
            );
        }

        usuarioMapper.updateEntity(request, usuario);
        Usuario updatedUsuario = usuarioRepository.save(usuario);

        return usuarioMapper.toResponse(updatedUsuario);
    }

    /**
     * Reemplaza la contraseña de un usuario por un hash BCrypt generado a partir
     * de la nueva contraseña recibida.
     */
    @Override
    @Transactional
    public void updatePassword(Long id, UsuarioPasswordRequest request) {
        Usuario usuario = findUsuarioById(id);
        String passwordHash = passwordEncoder.encode(request.nuevaPassword());

        usuario.setPasswordHash(passwordHash);
        usuarioRepository.save(usuario);
    }

    /**
     * Elimina un usuario después de comprobar que existe.
     */
    @Override
    @Transactional
    public void deleteUser(Long id) {
        Usuario usuario = findUsuarioById(id);
        usuarioRepository.delete(usuario);
    }

    /**
     * Asigna un rol existente a un usuario. La comprobación se realiza por
     * identificador para no depender de la identidad de las instancias JPA.
     */
    @Override
    @Transactional
    public UsuarioResponse assignRole(Long usuarioId, UsuarioRolRequest request) {
        Usuario usuario = findUsuarioById(usuarioId);
        Rol rol = findRolById(request.rolId());

        if (hasRole(usuario, rol.getId())) {
            throw new ConflictException(
                    "El usuario con identificador '%s' ya tiene asignado el rol '%s'"
                            .formatted(usuarioId, rol.getNombre())
            );
        }

        usuario.getRoles().add(rol);
        Usuario updatedUsuario = usuarioRepository.save(usuario);

        return usuarioMapper.toResponse(updatedUsuario);
    }

    /**
     * Retira un rol de un usuario comparando los identificadores de los roles
     * asociados. Si la relación no existe, informa un conflicto de negocio.
     */
    @Override
    @Transactional
    public void removeRole(Long usuarioId, Long rolId) {
        Usuario usuario = findUsuarioById(usuarioId);
        Rol rol = findRolById(rolId);

        boolean removed = usuario.getRoles()
                .removeIf(assignedRole -> Objects.equals(assignedRole.getId(), rolId));

        if (!removed) {
            throw new ConflictException(
                    "El usuario con identificador '%s' no tiene asignado el rol '%s'"
                            .formatted(usuarioId, rol.getNombre())
            );
        }

        usuarioRepository.save(usuario);
    }

    /**
     * Asigna un estacionamiento existente a un usuario. La relación se comprueba
     * por identificador para evitar duplicados aunque las instancias JPA difieran.
     */
    @Override
    @Transactional
    public UsuarioResponse assignEstacionamiento(Long usuarioId, UsuarioEstacionamientoRequest request) {
        Usuario usuario = findUsuarioById(usuarioId);
        Estacionamiento estacionamiento = findEstacionamientoById(request.estacionamientoId());

        if (hasEstacionamiento(usuario, estacionamiento.getId())) {
            throw new ConflictException(
                    "El usuario con identificador '%s' ya tiene asignado el estacionamiento '%s'"
                            .formatted(usuarioId, estacionamiento.getNombre())
            );
        }

        usuario.getEstacionamientos().add(estacionamiento);
        Usuario updatedUsuario = usuarioRepository.save(usuario);

        return usuarioMapper.toResponse(updatedUsuario);
    }

    /**
     * Retira un estacionamiento de un usuario comparando sus identificadores. Si
     * la relación no existe, informa un conflicto de negocio.
     */
    @Override
    @Transactional
    public void removeEstacionamiento(Long usuarioId, Long estacionamientoId) {
        Usuario usuario = findUsuarioById(usuarioId);
        Estacionamiento estacionamiento = findEstacionamientoById(estacionamientoId);

        boolean removed = usuario.getEstacionamientos()
                .removeIf(assigned -> Objects.equals(assigned.getId(), estacionamientoId));

        if (!removed) {
            throw new ConflictException(
                    "El usuario con identificador '%s' no tiene asignado el estacionamiento '%s'"
                            .formatted(usuarioId, estacionamiento.getNombre())
            );
        }

        usuarioRepository.save(usuario);
    }

    /**
     * Busca internamente un usuario o lanza una excepción 404.
     */
    private Usuario findUsuarioById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    /**
     * Busca internamente un rol o lanza una excepción cuando no existe.
     */
    private Rol findRolById(Long rolId) {
        return rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", rolId));
    }

    /**
     * Busca internamente un rol por nombre. Se utiliza para localizar roles base
     * creados por Flyway, como USER, sin depender de identificadores fijos.
     */
    private Rol findRolByNombre(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", nombre));
    }

    /**
     * Busca internamente un estacionamiento o lanza una excepción cuando no existe.
     */
    private Estacionamiento findEstacionamientoById(Long estacionamientoId) {
        return estacionamientoRepository.findById(estacionamientoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estacionamiento",
                        estacionamientoId
                ));
    }

    /**
     * Indica si un usuario ya tiene asignado un rol con el identificador recibido.
     */
    private boolean hasRole(Usuario usuario, Long rolId) {
        return usuario.getRoles()
                .stream()
                .anyMatch(assignedRole -> Objects.equals(assignedRole.getId(), rolId));
    }

    /**
     * Indica si el usuario ya tiene asignado un estacionamiento por identificador.
     */
    private boolean hasEstacionamiento(Usuario usuario, Long estacionamientoId) {
        return usuario.getEstacionamientos()
                .stream()
                .anyMatch(assigned -> Objects.equals(assigned.getId(), estacionamientoId));
    }

    /**
     * Verifica que el correo no pertenezca a otro usuario.
     */
    private void validateUniqueEmail(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictException(
                    "Ya existe un usuario con el correo '%s'".formatted(email)
            );
        }
    }
}
