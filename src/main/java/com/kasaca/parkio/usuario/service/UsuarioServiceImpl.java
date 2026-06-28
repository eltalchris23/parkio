package com.kasaca.parkio.usuario.service;

import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.usuario.dto.UsuarioRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.mapper.UsuarioMapper;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
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
     * Busca internamente un usuario o lanza una excepción 404.
     */
    private Usuario findUsuarioById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
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
