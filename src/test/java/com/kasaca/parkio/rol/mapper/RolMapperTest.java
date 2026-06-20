package com.kasaca.parkio.rol.mapper;

import com.kasaca.parkio.rol.dto.RolRequest;
import com.kasaca.parkio.rol.dto.RolResponse;
import com.kasaca.parkio.rol.entity.Rol;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class RolMapperTest {

    private final RolMapper rolMapper = new RolMapper();

    @Test
    void debeConvertirRequestAEntidad() {
        RolRequest request = new RolRequest("ADMIN", true);

        Rol rol = rolMapper.toEntity(request);

        assertThat(rol.getNombre()).isEqualTo("ADMIN");
        assertThat(rol.getActivo()).isTrue();
        assertThat(rol.getUsuarios()).isEmpty();
    }

    @Test
    void debeActualizarEntidadExistente() {
        Rol rol = new Rol();
        rol.setNombre("OPERADOR");
        rol.setActivo(true);

        RolRequest request = new RolRequest("SUPERVISOR", false);

        rolMapper.updateEntity(request, rol);

        assertThat(rol.getNombre()).isEqualTo("SUPERVISOR");
        assertThat(rol.getActivo()).isFalse();
    }

    @Test
    void debeConvertirEntidadAResponse() {
        LocalDateTime fechaCreacion =
                LocalDateTime.of(2026, 6, 20, 12, 0);

        Rol rol = new Rol();
        rol.setId(1L);
        rol.setNombre("ADMIN");
        rol.setActivo(true);
        rol.setFechaCreacion(fechaCreacion);

        RolResponse response = rolMapper.toResponse(rol);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("ADMIN");
        assertThat(response.activo()).isTrue();
        assertThat(response.fechaCreacion()).isEqualTo(fechaCreacion);
    }
}
