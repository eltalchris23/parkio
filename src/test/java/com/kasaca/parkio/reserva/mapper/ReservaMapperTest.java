package com.kasaca.parkio.reserva.mapper;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.entity.TipoCajon;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.reserva.dto.ReservaResponse;
import com.kasaca.parkio.reserva.entity.EstadoReserva;
import com.kasaca.parkio.reserva.entity.Reserva;
import com.kasaca.parkio.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservaMapperTest {

    private final ReservaMapper reservaMapper = new ReservaMapper();

    /**
     * Verifica que el mapper construya una entidad Reserva nueva con los datos calculados por el service.
     */
    @Test
    void debeConvertirDatosCalculadosAEntidad() {
        Usuario usuario = crearUsuario();
        Estacionamiento estacionamiento = crearEstacionamiento();
        Cajon cajon = crearCajon(estacionamiento);
        LocalDateTime fechaReserva = LocalDateTime.of(2026, 7, 25, 10, 0);
        LocalDateTime fechaExpiracion = fechaReserva.plusMinutes(20);

        Reserva reserva = reservaMapper.toEntity(
                "RSV-ABC12345",
                "ABC123",
                fechaReserva,
                fechaExpiracion,
                20,
                usuario,
                estacionamiento,
                cajon
        );

        assertThat(reserva.getCodigo()).isEqualTo("RSV-ABC12345");
        assertThat(reserva.getPlaca()).isEqualTo("ABC123");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CREADA);
        assertThat(reserva.getFechaReserva()).isEqualTo(fechaReserva);
        assertThat(reserva.getFechaExpiracion()).isEqualTo(fechaExpiracion);
        assertThat(reserva.getTiempoExpiracionMinutos()).isEqualTo(20);
        assertThat(reserva.getUsuario()).isEqualTo(usuario);
        assertThat(reserva.getEstacionamiento()).isEqualTo(estacionamiento);
        assertThat(reserva.getCajon()).isEqualTo(cajon);
    }

    /**
     * Verifica que el mapper convierta una entidad Reserva en el DTO publico de respuesta.
     */
    @Test
    void debeConvertirEntidadAResponse() {
        Usuario usuario = crearUsuario();
        Estacionamiento estacionamiento = crearEstacionamiento();
        Cajon cajon = crearCajon(estacionamiento);
        Reserva reserva = crearReserva(usuario, estacionamiento, cajon);

        ReservaResponse response = reservaMapper.toResponse(reserva);

        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.codigo()).isEqualTo("RSV-ABC12345");
        assertThat(response.placa()).isEqualTo("ABC123");
        assertThat(response.estado()).isEqualTo(EstadoReserva.CREADA);
        assertThat(response.usuarioId()).isEqualTo(1L);
        assertThat(response.estacionamientoId()).isEqualTo(10L);
        assertThat(response.cajonId()).isEqualTo(20L);
        assertThat(response.activo()).isTrue();
    }

    /**
     * Crea un usuario de prueba para relacionarlo con la reserva.
     */
    private Usuario crearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Cliente");
        usuario.setEmail("cliente.reserva@parkio.com");
        usuario.setActivo(true);
        return usuario;
    }

    /**
     * Crea un estacionamiento de prueba para relacionarlo con la reserva.
     */
    private Estacionamiento crearEstacionamiento() {
        Estacionamiento estacionamiento = new Estacionamiento();
        estacionamiento.setId(10L);
        estacionamiento.setNombre("Estacionamiento Reserva");
        estacionamiento.setActivo(true);
        return estacionamiento;
    }

    /**
     * Crea un cajon libre de prueba para relacionarlo con la reserva.
     */
    private Cajon crearCajon(Estacionamiento estacionamiento) {
        Cajon cajon = new Cajon();
        cajon.setId(20L);
        cajon.setNumero("A-01");
        cajon.setTipo(TipoCajon.AUTO);
        cajon.setEstado(EstadoCajon.LIBRE);
        cajon.setEstacionamiento(estacionamiento);
        cajon.setActivo(true);
        return cajon;
    }

    /**
     * Crea una reserva completa de prueba para convertirla a DTO.
     */
    private Reserva crearReserva(
            Usuario usuario,
            Estacionamiento estacionamiento,
            Cajon cajon
    ) {
        Reserva reserva = Reserva.builder()
                .codigo("RSV-ABC12345")
                .placa("ABC123")
                .estado(EstadoReserva.CREADA)
                .fechaReserva(LocalDateTime.of(2026, 7, 25, 10, 0))
                .fechaExpiracion(LocalDateTime.of(2026, 7, 25, 10, 20))
                .tiempoExpiracionMinutos(20)
                .usuario(usuario)
                .estacionamiento(estacionamiento)
                .cajon(cajon)
                .build();

        reserva.setId(30L);
        reserva.setActivo(true);
        reserva.setFechaCreacion(LocalDateTime.of(2026, 7, 25, 10, 0));
        return reserva;
    }
}
