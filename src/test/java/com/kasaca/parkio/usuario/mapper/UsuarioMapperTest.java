package com.kasaca.parkio.usuario.mapper;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.rol.entity.Rol;
import com.kasaca.parkio.usuario.dto.UsuarioRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioMapperTest {

    private final UsuarioMapper usuarioMapper = new UsuarioMapper();

    /**
     * Verifica que los datos de entrada y el hash generado por el servicio se
     * copien correctamente a una entidad nueva.
     */
    @Test
    void debeConvertirRequestAEntidad() {
        UsuarioRequest request = crearRequest();

        Usuario usuario = usuarioMapper.toEntity(request, "hash-seguro");

        assertThat(usuario.getNombre()).isEqualTo("Christian");
        assertThat(usuario.getApellido()).isEqualTo("Salazar");
        assertThat(usuario.getEmail()).isEqualTo("christian@parkio.com");
        assertThat(usuario.getPasswordHash()).isEqualTo("hash-seguro");
        assertThat(usuario.getRoles()).isEmpty();
        assertThat(usuario.getEstacionamientos()).isEmpty();
    }

    /**
     * Comprueba que la actualización modifica únicamente los datos administrados
     * por el mapper y conserva las asociaciones existentes.
     */
    @Test
    void debeActualizarEntidadExistente() {
        Usuario usuario = crearUsuario();
        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        usuario.getRoles().add(rol);
        Estacionamiento estacionamiento = new Estacionamiento();
        estacionamiento.setId(3L);
        usuario.getEstacionamientos().add(estacionamiento);

        UsuarioRequest request = new UsuarioRequest("Nuevo", "Nombre", "nuevo@parkio.com", "otra-clave");

        usuarioMapper.updateEntity(request, usuario, "hash-actualizado");

        assertThat(usuario.getNombre()).isEqualTo("Nuevo");
        assertThat(usuario.getApellido()).isEqualTo("Nombre");
        assertThat(usuario.getEmail()).isEqualTo("nuevo@parkio.com");
        assertThat(usuario.getPasswordHash()).isEqualTo("hash-actualizado");
        assertThat(usuario.getRoles()).containsExactly(rol);
        assertThat(usuario.getEstacionamientos()).containsExactly(estacionamiento);
    }

    /**
     * Verifica que la respuesta contenga solamente datos públicos y los nombres
     * de los roles asociados.
     */
    @Test
    void debeConvertirEntidadAResponse() {
        Usuario usuario = crearUsuario();
        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        usuario.getRoles().add(rol);
        Estacionamiento estacionamiento = new Estacionamiento();
        estacionamiento.setId(3L);
        usuario.getEstacionamientos().add(estacionamiento);

        UsuarioResponse response = usuarioMapper.toResponse(usuario);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("Christian");
        assertThat(response.apellido()).isEqualTo("Salazar");
        assertThat(response.email()).isEqualTo("christian@parkio.com");
        assertThat(response.activo()).isTrue();
        assertThat(response.fechaCreacion()).isEqualTo(usuario.getFechaCreacion());
        assertThat(response.roles()).containsExactly("ADMIN");
        assertThat(response.estacionamientoIds()).containsExactly(3L);
    }

    /**
     * Construye una solicitud válida reutilizable por las pruebas del mapper.
     */
    private UsuarioRequest crearRequest() {
        return new UsuarioRequest("Christian", "Salazar", "christian@parkio.com", "clave-segura");
    }

    /**
     * Construye una entidad completa reutilizable por las pruebas del mapper.
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
}
