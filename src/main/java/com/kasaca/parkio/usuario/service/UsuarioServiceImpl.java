package com.kasaca.parkio.usuario.service;

import com.kasaca.parkio.rol.entity.Rol;
import com.kasaca.parkio.rol.repository.RolRepository;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.usuario.dto.UsuarioRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.dto.UsuarioRolRequest;
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

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
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
     * Crea un usuario después de validar el correo y cifrar su contraseña.
     */
    @Override
    @Transactional
    public UsuarioResponse addUser(UsuarioRequest request) {
        validateUniqueEmail(request.email());

        String passwordHash = passwordEncoder.encode(request.password());
        Usuario usuario = usuarioMapper.toEntity(request, passwordHash);
        Usuario savedUsuario = usuarioRepository.save(usuario);

        return usuarioMapper.toResponse(savedUsuario);
    }

    /**
     * Actualiza los datos de un usuario existente y genera un nuevo hash para la
     * contraseña recibida.
     */
    @Override
    @Transactional
    public UsuarioResponse updateUser(Long id, UsuarioRequest request) {
        Usuario usuario = findUsuarioById(id);

        if (usuarioRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ConflictException(
                    "Ya existe un usuario con el correo '%s'".formatted(request.email())
            );
        }

        String passwordHash = passwordEncoder.encode(request.password());
        usuarioMapper.updateEntity(request, usuario, passwordHash);
        Usuario updatedUsuario = usuarioRepository.save(usuario);

        return usuarioMapper.toResponse(updatedUsuario);
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
     * Indica si un usuario ya tiene asignado un rol con el identificador recibido.
     */
    private boolean hasRole(Usuario usuario, Long rolId) {
        return usuario.getRoles()
                .stream()
                .anyMatch(assignedRole -> Objects.equals(assignedRole.getId(), rolId));
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
