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
    private RolRepository rolRepository;

    @Mock
    private EstacionamientoRepository estacionamientoRepository;

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
     * Comprueba que la creación valide el correo, genere el hash, asigne el rol
     * base USER y persista la entidad.
     */
    @Test
    void debeCrearUsuarioConPasswordCifradoYRolUser() {
        UsuarioCreateRequest request = crearCreateRequest();
        Usuario usuario = crearUsuario();
        Rol rolUser = crearRolUser();
        UsuarioResponse response = crearResponse();

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hash-seguro");
        when(usuarioMapper.toEntity(request, "hash-seguro")).thenReturn(usuario);
        when(rolRepository.findByNombre("USER")).thenReturn(Optional.of(rolUser));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);

        UsuarioResponse resultado = usuarioService.addUser(request);

        assertThat(resultado).isEqualTo(response);
        assertThat(usuario.getRoles()).containsExactly(rolUser);
        verify(passwordEncoder).encode(request.password());
        verify(rolRepository).findByNombre("USER");
        verify(usuarioRepository).save(usuario);
    }

    /**
     * Verifica que la creación falle de forma controlada si no existe el rol
     * base USER requerido para el registro público.
     */
    @Test
    void debeRechazarCreacionCuandoRolUserNoExiste() {
        UsuarioCreateRequest request = crearCreateRequest();
        Usuario usuario = crearUsuario();

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hash-seguro");
        when(usuarioMapper.toEntity(request, "hash-seguro")).thenReturn(usuario);
        when(rolRepository.findByNombre("USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.addUser(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Rol con identificador 'USER' no fue encontrado");

        verify(usuarioRepository, never()).save(any());
    }

    /**
     * Verifica que no se cifre ni persista cuando el correo ya está registrado.
     */
    @Test
    void debeRechazarCorreoDuplicadoAlCrear() {
        UsuarioCreateRequest request = crearCreateRequest();
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
        UsuarioUpdateRequest request = crearUpdateRequest();
        Usuario usuario = crearUsuario();
        UsuarioResponse response = crearResponse();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);

        UsuarioResponse resultado = usuarioService.updateUser(1L, request);

        assertThat(resultado).isEqualTo(response);
        verify(usuarioMapper).updateEntity(request, usuario);
        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository).save(usuario);
    }

    /**
     * Confirma que actualizar con el correo de otro usuario produzca un conflicto.
     */
    @Test
    void debeRechazarCorreoDuplicadoAlActualizar() {
        UsuarioUpdateRequest request = crearUpdateRequest();
        Usuario usuario = crearUsuario();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.updateUser(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un usuario con el correo 'christian@parkio.com'");

        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioMapper, never()).updateEntity(any(), any());
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
     * Verifica que un rol existente se agregue al usuario y se devuelva la
     * respuesta actualizada.
     */
    @Test
    void debeAsignarRolAUsuario() {
        Usuario usuario = crearUsuario();
        Rol rol = crearRol();
        UsuarioRolRequest request = new UsuarioRolRequest(2L);
        UsuarioResponse response = crearResponse();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(2L)).thenReturn(Optional.of(rol));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);

        UsuarioResponse resultado = usuarioService.assignRole(1L, request);

        assertThat(resultado).isEqualTo(response);
        assertThat(usuario.getRoles()).containsExactly(rol);
        verify(usuarioRepository).save(usuario);
    }

    /**
     * Comprueba que la asignación sea rechazada cuando otro objeto Rol con el
     * mismo identificador ya está asociado al usuario.
     */
    @Test
    void debeRechazarRolDuplicadoPorIdentificador() {
        Usuario usuario = crearUsuario();
        Rol rolAsignado = crearRol();
        Rol rolEncontrado = crearRol();
        usuario.getRoles().add(rolAsignado);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(2L)).thenReturn(Optional.of(rolEncontrado));

        assertThatThrownBy(() -> usuarioService.assignRole(1L, new UsuarioRolRequest(2L)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El usuario con identificador '1' ya tiene asignado el rol 'ADMIN'");

        verify(usuarioRepository, never()).save(any());
    }

    /**
     * Confirma que no se modifique el usuario cuando el rol solicitado no existe.
     */
    @Test
    void debeRechazarAsignacionCuandoRolNoExiste() {
        Usuario usuario = crearUsuario();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.assignRole(1L, new UsuarioRolRequest(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Rol con identificador '99' no fue encontrado");

        verify(usuarioRepository, never()).save(any());
    }

    /**
     * Verifica que el retiro elimine la relación comparando identificadores,
     * incluso cuando las instancias de Rol sean distintas.
     */
    @Test
    void debeRetirarRolPorIdentificador() {
        Usuario usuario = crearUsuario();
        Rol rolAsignado = crearRol();
        Rol rolEncontrado = crearRol();
        usuario.getRoles().add(rolAsignado);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(2L)).thenReturn(Optional.of(rolEncontrado));

        usuarioService.removeRole(1L, 2L);

        assertThat(usuario.getRoles()).isEmpty();
        verify(usuarioRepository).save(usuario);
    }

    /**
     * Comprueba que retirar un rol no asignado produzca un conflicto y no guarde
     * cambios en el usuario.
     */
    @Test
    void debeRechazarRetiroDeRolNoAsignado() {
        Usuario usuario = crearUsuario();
        Rol rol = crearRol();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(2L)).thenReturn(Optional.of(rol));

        assertThatThrownBy(() -> usuarioService.removeRole(1L, 2L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El usuario con identificador '1' no tiene asignado el rol 'ADMIN'");

        verify(usuarioRepository, never()).save(any());
    }

    /**
     * Verifica que un estacionamiento existente se asigne al usuario y se
     * devuelva la respuesta actualizada.
     */
    @Test
    void debeAsignarEstacionamientoAUsuario() {
        Usuario usuario = crearUsuario();
        Estacionamiento estacionamiento = crearEstacionamiento();
        UsuarioEstacionamientoRequest request = new UsuarioEstacionamientoRequest(3L);
        UsuarioResponse response = crearResponse();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(estacionamientoRepository.findById(3L)).thenReturn(Optional.of(estacionamiento));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);

        UsuarioResponse resultado = usuarioService.assignEstacionamiento(1L, request);

        assertThat(resultado).isEqualTo(response);
        assertThat(usuario.getEstacionamientos()).containsExactly(estacionamiento);
        verify(usuarioRepository).save(usuario);
    }

    /**
     * Comprueba que se rechace un estacionamiento duplicado comparando su
     * identificador y que no se guarden cambios.
     */
    @Test
    void debeRechazarEstacionamientoDuplicadoPorIdentificador() {
        Usuario usuario = crearUsuario();
        usuario.getEstacionamientos().add(crearEstacionamiento());
        Estacionamiento estacionamientoEncontrado = crearEstacionamiento();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(estacionamientoRepository.findById(3L)).thenReturn(Optional.of(estacionamientoEncontrado));

        assertThatThrownBy(() -> usuarioService.assignEstacionamiento(
                1L,
                new UsuarioEstacionamientoRequest(3L)
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El usuario con identificador '1' ya tiene asignado el estacionamiento 'Parkio Centro'");

        verify(usuarioRepository, never()).save(any());
    }

    /**
     * Confirma que un estacionamiento inexistente produzca una excepción 404.
     */
    @Test
    void debeRechazarAsignacionCuandoEstacionamientoNoExiste() {
        Usuario usuario = crearUsuario();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(estacionamientoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.assignEstacionamiento(
                1L,
                new UsuarioEstacionamientoRequest(99L)
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Estacionamiento con identificador '99' no fue encontrado");

        verify(usuarioRepository, never()).save(any());
    }

    /**
     * Verifica que se retire un estacionamiento por identificador aunque la
     * instancia recuperada sea diferente a la asociada.
     */
    @Test
    void debeRetirarEstacionamientoPorIdentificador() {
        Usuario usuario = crearUsuario();
        usuario.getEstacionamientos().add(crearEstacionamiento());
        Estacionamiento estacionamientoEncontrado = crearEstacionamiento();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(estacionamientoRepository.findById(3L)).thenReturn(Optional.of(estacionamientoEncontrado));

        usuarioService.removeEstacionamiento(1L, 3L);

        assertThat(usuario.getEstacionamientos()).isEmpty();
        verify(usuarioRepository).save(usuario);
    }

    /**
     * Comprueba que retirar un estacionamiento no asignado produzca un conflicto.
     */
    @Test
    void debeRechazarRetiroDeEstacionamientoNoAsignado() {
        Usuario usuario = crearUsuario();
        Estacionamiento estacionamiento = crearEstacionamiento();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(estacionamientoRepository.findById(3L)).thenReturn(Optional.of(estacionamiento));

        assertThatThrownBy(() -> usuarioService.removeEstacionamiento(1L, 3L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El usuario con identificador '1' no tiene asignado el estacionamiento 'Parkio Centro'");

        verify(usuarioRepository, never()).save(any());
    }

    /**
     * Verifica que el cambio de contraseña genere un hash nuevo y lo persista sin
     * modificar los demás datos del usuario.
     */
    @Test
    void debeActualizarPasswordConHashSeguro() {
        Usuario usuario = crearUsuario();
        UsuarioPasswordRequest request = new UsuarioPasswordRequest("nueva-clave");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("nueva-clave")).thenReturn("hash-nuevo");

        usuarioService.updatePassword(1L, request);

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-nuevo");
        verify(passwordEncoder).encode("nueva-clave");
        verify(usuarioRepository).save(usuario);
    }

    /**
     * Confirma que no se genere un hash cuando se intenta cambiar la contraseña
     * de un usuario inexistente.
     */
    @Test
    void debeRechazarCambioPasswordCuandoUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.updatePassword(
                99L,
                new UsuarioPasswordRequest("nueva-clave")
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuario con identificador '99' no fue encontrado");

        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any());
    }

    /**
     * Construye una solicitud válida reutilizable por las pruebas del servicio.
     */
    private UsuarioCreateRequest crearCreateRequest() {
        return new UsuarioCreateRequest("Christian", "Salazar", "christian@parkio.com", "clave-segura");
    }

    /**
     * Construye una solicitud válida para actualizar datos generales.
     */
    private UsuarioUpdateRequest crearUpdateRequest() {
        return new UsuarioUpdateRequest("Christian", "Salazar", "christian@parkio.com");
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
     * Construye un rol válido reutilizable por las pruebas de asignación.
     */
    private Rol crearRol() {
        Rol rol = new Rol();
        rol.setId(2L);
        rol.setNombre("ADMIN");
        rol.setActivo(true);
        return rol;
    }

    /**
     * Construye el rol base USER utilizado durante el registro público de usuarios.
     */
    private Rol crearRolUser() {
        Rol rol = new Rol();
        rol.setId(4L);
        rol.setNombre("USER");
        rol.setActivo(true);
        return rol;
    }

    /**
     * Construye un estacionamiento válido para las pruebas de asignación.
     */
    private Estacionamiento crearEstacionamiento() {
        Estacionamiento estacionamiento = new Estacionamiento();
        estacionamiento.setId(3L);
        estacionamiento.setNombre("Parkio Centro");
        estacionamiento.setActivo(true);
        return estacionamiento;
    }

    /**
     * Construye una respuesta pública reutilizable por las pruebas del servicio.
     */
    private UsuarioResponse crearResponse() {
        return new UsuarioResponse(1L, "Christian", "Salazar", "christian@parkio.com", true,
                LocalDateTime.of(2026, 6, 28, 12, 0), Set.of(), Set.of());
    }
}
