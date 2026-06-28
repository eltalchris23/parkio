package com.kasaca.parkio.usuario.service;

import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.usuario.dto.UsuarioRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.mapper.UsuarioMapper;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    /**
     * Comprueba que el servicio liste y convierta todos los usuarios encontrados.
     */
    @Test
    void debeObtenerTodosLosUsuarios() {
        Usuario usuario = crearUsuario();
        UsuarioResponse response = crearResponse();

        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);

        List<UsuarioResponse> resultado = usuarioService.getAllUsers();

        assertThat(resultado).containsExactly(response);
        verify(usuarioRepository).findAll();
        verify(usuarioMapper).toResponse(usuario);
    }

    /**
     * Verifica que se obtenga y convierta un usuario existente por identificador.
     */
    @Test
    void debeObtenerUsuarioPorId() {
        Usuario usuario = crearUsuario();
        UsuarioResponse response = crearResponse();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);

        UsuarioResponse resultado = usuarioService.getUserById(1L);

        assertThat(resultado).isEqualTo(response);
    }

    /**
     * Confirma que consultar un usuario inexistente produzca una excepción 404.
     */
    @Test
    void debeLanzarExcepcionCuandoUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuario con identificador '99' no fue encontrado");

        verify(usuarioMapper, never()).toResponse(any());
    }

    /**
     * Comprueba que la creación valide el correo, genere el hash y persista la entidad.
     */
    @Test
    void debeCrearUsuarioConPasswordCifrado() {
        UsuarioRequest request = crearRequest();
        Usuario usuario = crearUsuario();
        UsuarioResponse response = crearResponse();

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hash-seguro");
        when(usuarioMapper.toEntity(request, "hash-seguro")).thenReturn(usuario);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);

        UsuarioResponse resultado = usuarioService.addUser(request);

        assertThat(resultado).isEqualTo(response);
        verify(passwordEncoder).encode(request.password());
        verify(usuarioRepository).save(usuario);
    }

    /**
     * Verifica que no se cifre ni persista cuando el correo ya está registrado.
     */
    @Test
    void debeRechazarCorreoDuplicadoAlCrear() {
        UsuarioRequest request = crearRequest();
        when(usuarioRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.addUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un usuario con el correo 'christian@parkio.com'");

        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any());
    }

    /**
     * Comprueba que la actualización genere un hash nuevo y persista los cambios.
     */
    @Test
    void debeActualizarUsuarioConPasswordCifrado() {
        UsuarioRequest request = crearRequest();
        Usuario usuario = crearUsuario();
        UsuarioResponse response = crearResponse();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hash-actualizado");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);

        UsuarioResponse resultado = usuarioService.updateUser(1L, request);

        assertThat(resultado).isEqualTo(response);
        verify(usuarioMapper).updateEntity(request, usuario, "hash-actualizado");
        verify(usuarioRepository).save(usuario);
    }

    /**
     * Confirma que actualizar con el correo de otro usuario produzca un conflicto.
     */
    @Test
    void debeRechazarCorreoDuplicadoAlActualizar() {
        UsuarioRequest request = crearRequest();
        Usuario usuario = crearUsuario();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.updateUser(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un usuario con el correo 'christian@parkio.com'");

        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioMapper, never()).updateEntity(any(), any(), anyString());
        verify(usuarioRepository, never()).save(any());
    }

    /**
     * Verifica que la eliminación utilice la entidad previamente localizada.
     */
    @Test
    void debeEliminarUsuario() {
        Usuario usuario = crearUsuario();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        usuarioService.deleteUser(1L);

        verify(usuarioRepository).delete(usuario);
    }

    /**
     * Confirma que no se invoque delete cuando el usuario no existe.
     */
    @Test
    void debeRechazarEliminacionCuandoUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuario con identificador '99' no fue encontrado");

        verify(usuarioRepository, never()).delete(any());
    }

    /**
     * Construye una solicitud válida reutilizable por las pruebas del servicio.
     */
    private UsuarioRequest crearRequest() {
        return new UsuarioRequest("Christian", "Salazar", "christian@parkio.com", "clave-segura");
    }

    /**
     * Construye una entidad válida reutilizable por las pruebas del servicio.
     */
    private Usuario crearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Christian");
        usuario.setApellido("Salazar");
        usuario.setEmail("christian@parkio.com");
        usuario.setPasswordHash("hash-seguro");
        usuario.setActivo(true);
        usuario.setFechaCreacion(LocalDateTime.of(2026, 6, 28, 12, 0));
        return usuario;
    }

    /**
     * Construye una respuesta pública reutilizable por las pruebas del servicio.
     */
    private UsuarioResponse crearResponse() {
        return new UsuarioResponse(1L, "Christian", "Salazar", "christian@parkio.com", true,
                LocalDateTime.of(2026, 6, 28, 12, 0), Set.of());
    }
}
