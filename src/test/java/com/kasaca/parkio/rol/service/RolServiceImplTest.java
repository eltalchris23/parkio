package com.kasaca.parkio.rol.service;

import com.kasaca.parkio.rol.dto.RolRequest;
import com.kasaca.parkio.rol.dto.RolResponse;
import com.kasaca.parkio.rol.entity.Rol;
import com.kasaca.parkio.rol.mapper.RolMapper;
import com.kasaca.parkio.rol.repository.RolRepository;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolServiceImplTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private RolMapper rolMapper;

    @InjectMocks
    private RolServiceImpl rolService;

    @Test
    void debeObtenerTodosLosRoles() {
        Rol rol = crearRol();
        RolResponse response = crearResponse();

        when(rolRepository.findAll()).thenReturn(List.of(rol));
        when(rolMapper.toResponse(rol)).thenReturn(response);

        List<RolResponse> resultado = rolService.getRoles();

        assertThat(resultado).containsExactly(response);
        verify(rolRepository).findAll();
        verify(rolMapper).toResponse(rol);
    }

    @Test
    void debeObtenerRolPorId() {
        Rol rol = crearRol();
        RolResponse response = crearResponse();

        when(rolRepository.findById(1L))
                .thenReturn(Optional.of(rol));
        when(rolMapper.toResponse(rol)).thenReturn(response);

        RolResponse resultado = rolService.getRol(1L);

        assertThat(resultado).isEqualTo(response);
    }

    @Test
    void debeLanzarExcepcionCuandoRolNoExiste() {
        when(rolRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolService.getRol(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Rol con identificador '1' no fue encontrado");

        verify(rolMapper, never()).toResponse(any());
    }

    @Test
    void debeCrearRol() {
        RolRequest request = new RolRequest("ADMIN", true);
        Rol rol = crearRol();
        RolResponse response = crearResponse();

        when(rolRepository.existsByNombre("ADMIN"))
                .thenReturn(false);
        when(rolMapper.toEntity(request)).thenReturn(rol);
        when(rolRepository.save(rol)).thenReturn(rol);
        when(rolMapper.toResponse(rol)).thenReturn(response);

        RolResponse resultado = rolService.addRol(request);

        assertThat(resultado).isEqualTo(response);
        verify(rolRepository).save(rol);
    }

    @Test
    void debeRechazarNombreDuplicadoAlCrear() {
        RolRequest request = new RolRequest("ADMIN", true);

        when(rolRepository.existsByNombre("ADMIN"))
                .thenReturn(true);

        assertThatThrownBy(() -> rolService.addRol(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un rol con el nombre 'ADMIN'");

        verify(rolMapper, never()).toEntity(any());
        verify(rolRepository, never()).save(any());
    }

    @Test
    void debeActualizarRol() {
        RolRequest request = new RolRequest("SUPERVISOR", false);
        Rol rol = crearRol();
        RolResponse response = new RolResponse(
                1L,
                "SUPERVISOR",
                false,
                rol.getFechaCreacion()
        );

        when(rolRepository.findById(1L))
                .thenReturn(Optional.of(rol));
        when(rolRepository.existsByNombreAndIdNot("SUPERVISOR", 1L))
                .thenReturn(false);
        when(rolRepository.save(rol)).thenReturn(rol);
        when(rolMapper.toResponse(rol)).thenReturn(response);

        RolResponse resultado = rolService.updateRol(1L, request);

        assertThat(resultado).isEqualTo(response);
        verify(rolMapper).updateEntity(request, rol);
        verify(rolRepository).save(rol);
    }

    @Test
    void debeRechazarNombreDuplicadoAlActualizar() {
        RolRequest request = new RolRequest("ADMIN", true);
        Rol rol = crearRol();

        when(rolRepository.findById(1L))
                .thenReturn(Optional.of(rol));
        when(rolRepository.existsByNombreAndIdNot("ADMIN", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> rolService.updateRol(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un rol con el nombre 'ADMIN'");

        verify(rolMapper, never()).updateEntity(any(), any());
        verify(rolRepository, never()).save(any());
    }

    @Test
    void debeEliminarRol() {
        Rol rol = crearRol();

        when(rolRepository.findById(1L))
                .thenReturn(Optional.of(rol));

        rolService.deleteRol(1L);

        verify(rolRepository).delete(rol);
    }

    @Test
    void debeRechazarEliminacionCuandoRolNoExiste() {
        when(rolRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolService.deleteRol(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Rol con identificador '1' no fue encontrado");

        verify(rolRepository, never()).delete(any());
    }

    private Rol crearRol() {
        Rol rol = new Rol();
        rol.setId(1L);
        rol.setNombre("ADMIN");
        rol.setActivo(true);
        rol.setFechaCreacion(
                LocalDateTime.of(2026, 6, 20, 12, 0)
        );
        return rol;
    }

    private RolResponse crearResponse() {
        return new RolResponse(
                1L,
                "ADMIN",
                true,
                LocalDateTime.of(2026, 6, 20, 12, 0)
        );
    }
}