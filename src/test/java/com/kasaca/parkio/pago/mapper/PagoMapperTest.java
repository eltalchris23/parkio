package com.kasaca.parkio.pago.mapper;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.pago.dto.PagoResponse;
import com.kasaca.parkio.pago.entity.EstadoPago;
import com.kasaca.parkio.pago.entity.MetodoPago;
import com.kasaca.parkio.pago.entity.Pago;
import com.kasaca.parkio.ticket.entity.EstadoTicket;
import com.kasaca.parkio.ticket.entity.Ticket;
import com.kasaca.parkio.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PagoMapperTest {

    private final PagoMapper pagoMapper = new PagoMapper();

    /**
     * Verifica que el mapper construya una entidad Pago usando el monto total
     * calculado previamente en el ticket y no un valor enviado por el frontend.
     */
    @Test
    void debeConvertirDatosAEntidad() {
        Ticket ticket = crearTicket();
        Usuario operador = crearUsuario(9L);
        LocalDateTime fechaPago = LocalDateTime.of(2026, 8, 1, 18, 30);

        Pago pago = pagoMapper.toEntity(
                ticket,
                operador,
                new BigDecimal("100.00"),
                new BigDecimal("27.50"),
                MetodoPago.EFECTIVO,
                fechaPago
        );

        assertThat(pago.getTicket()).isEqualTo(ticket);
        assertThat(pago.getOperador()).isEqualTo(operador);
        assertThat(pago.getMontoTotal()).isEqualByComparingTo("72.50");
        assertThat(pago.getMontoRecibido()).isEqualByComparingTo("100.00");
        assertThat(pago.getCambio()).isEqualByComparingTo("27.50");
        assertThat(pago.getMetodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.REGISTRADO);
        assertThat(pago.getFechaPago()).isEqualTo(fechaPago);
    }

    /**
     * Verifica que el mapper genere un response sin serializar entidades JPA completas.
     */
    @Test
    void debeConvertirEntidadAResponse() {
        Pago pago = crearPago();

        PagoResponse response = pagoMapper.toResponse(pago);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.ticketId()).isEqualTo(3L);
        assertThat(response.codigoTicket()).isEqualTo("TCK-ABC12345");
        assertThat(response.montoTotal()).isEqualByComparingTo("72.50");
        assertThat(response.montoRecibido()).isEqualByComparingTo("100.00");
        assertThat(response.cambio()).isEqualByComparingTo("27.50");
        assertThat(response.metodoPago()).isEqualTo("EFECTIVO");
        assertThat(response.estado()).isEqualTo("REGISTRADO");
        assertThat(response.operadorId()).isEqualTo(9L);
        assertThat(response.activo()).isTrue();
    }

    /**
     * Construye un pago completo para validar el response.
     */
    private Pago crearPago() {
        Pago pago = new Pago();
        pago.setId(5L);
        pago.setTicket(crearTicket());
        pago.setOperador(crearUsuario(9L));
        pago.setMontoTotal(new BigDecimal("72.50"));
        pago.setMontoRecibido(new BigDecimal("100.00"));
        pago.setCambio(new BigDecimal("27.50"));
        pago.setMetodoPago(MetodoPago.EFECTIVO);
        pago.setEstado(EstadoPago.REGISTRADO);
        pago.setFechaPago(LocalDateTime.of(2026, 8, 1, 18, 30));
        pago.setActivo(true);
        pago.setFechaCreacion(LocalDateTime.of(2026, 8, 1, 18, 30));
        return pago;
    }

    /**
     * Construye un ticket pendiente de pago con monto calculado.
     */
    private Ticket crearTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(3L);
        ticket.setCodigo("TCK-ABC12345");
        ticket.setEstado(EstadoTicket.PENDIENTE_PAGO);
        ticket.setMontoTotal(new BigDecimal("72.50"));
        ticket.setEstacionamiento(crearEstacionamiento());
        ticket.setCajon(crearCajon());
        return ticket;
    }

    /**
     * Construye un usuario minimo con identificador.
     */
    private Usuario crearUsuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setActivo(true);
        return usuario;
    }

    /**
     * Construye un estacionamiento minimo para completar la relacion del ticket.
     */
    private Estacionamiento crearEstacionamiento() {
        Estacionamiento estacionamiento = new Estacionamiento();
        estacionamiento.setId(1L);
        return estacionamiento;
    }

    /**
     * Construye un cajon minimo para completar la relacion del ticket.
     */
    private Cajon crearCajon() {
        Cajon cajon = new Cajon();
        cajon.setId(2L);
        return cajon;
    }
}
