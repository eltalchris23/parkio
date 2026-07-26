package com.kasaca.parkio.ticket.entity;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.reserva.entity.Reserva;
import com.kasaca.parkio.shared.entity.BaseEntity;
import com.kasaca.parkio.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Representa el ticket generado cuando un cliente llega al estacionamiento
 * y una reserva vigente se convierte en ocupacion real de un cajon.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket")
public class Ticket extends BaseEntity {

    /**
     * Codigo publico del ticket que puede usarse para identificar la estancia.
     */
    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    /**
     * Estado actual del ticket dentro de su ciclo de vida.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoTicket estado = EstadoTicket.ABIERTO;

    /**
     * Placa del vehiculo asociada al ticket.
     *
     * <p>Inicialmente puede copiarse desde la reserva.</p>
     */
    @Column(name = "placa", length = 15)
    private String placa;

    /**
     * Fecha y hora en que el vehiculo ingreso al estacionamiento.
     */
    @Column(name = "fecha_entrada", nullable = false)
    private LocalDateTime fechaEntrada;

    /**
     * Fecha y hora en que el vehiculo salio del estacionamiento.
     *
     * <p>Permanece nula mientras el ticket esta ABIERTO.</p>
     */
    @Column(name = "fecha_salida")
    private LocalDateTime fechaSalida;

    /**
     * Reserva que origino este ticket.
     *
     * <p>Debe ser unica porque una reserva solo puede convertirse en un ticket.</p>
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reserva_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_ticket_reserva")
    )
    private Reserva reserva;

    /**
     * Cliente dueño de la reserva que origino el ticket.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "usuario_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ticket_usuario")
    )
    private Usuario usuario;

    /**
     * Operador que registro la entrada del vehiculo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "operador_entrada_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ticket_operador_entrada")
    )
    private Usuario operadorEntrada;

    /**
     * Estacionamiento donde se genera el ticket.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "estacionamiento_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ticket_estacionamiento")
    )
    private Estacionamiento estacionamiento;

    /**
     * Cajon ocupado por el vehiculo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cajon_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ticket_cajon")
    )
    private Cajon cajon;
}
