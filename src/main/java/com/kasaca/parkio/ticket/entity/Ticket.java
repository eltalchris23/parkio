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

import java.math.BigDecimal;
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
     * Minutos totales calculados entre la entrada y la salida.
     *
     * <p>Se guarda para conservar evidencia del calculo usado al registrar la salida.</p>
     */
    @Column(name = "minutos_estancia")
    private Integer minutosEstancia;

    /**
     * Importe final calculado para la estancia.
     *
     * <p>Se calcula al registrar la salida usando la tarifa activa del estacionamiento.</p>
     */
    @Column(name = "monto_total", precision = 10, scale = 2)
    private BigDecimal montoTotal;

    /**
     * Precio por hora vigente que se aplico al registrar la salida.
     *
     * <p>Se guarda como fotografia historica para que futuros cambios de tarifa
     * no alteren el cobro de tickets ya cerrados.</p>
     */
    @Column(name = "precio_por_hora_aplicado", precision = 10, scale = 2)
    private BigDecimal precioPorHoraAplicado;

    /**
     * Minutos de tolerancia vigentes que se aplicaron al registrar la salida.
     */
    @Column(name = "minutos_tolerancia_aplicados")
    private Integer minutosToleranciaAplicados;

    /**
     * Indica si la fraccion de hora se cobro como hora completa al registrar la salida.
     */
    @Column(name = "cobrar_fraccion_aplicado")
    private Boolean cobrarFraccionAplicado;

    /**
     * Tarifa minima vigente que se aplico al registrar la salida.
     */
    @Column(name = "tarifa_minima_aplicada", precision = 10, scale = 2)
    private BigDecimal tarifaMinimaAplicada;

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
