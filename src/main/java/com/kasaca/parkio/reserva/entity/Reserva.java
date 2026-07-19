package com.kasaca.parkio.reserva.entity;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Representa una reserva temporal de un cajon realizada por un cliente.
 *
 * <p>La reserva aparta un cajon durante una ventana corta de tiempo. Si el
 * cliente no llega antes de la expiracion, la reserva podra marcarse como
 * EXPIRADA y el cajon debera volver a estar disponible.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reserva")
public class Reserva extends BaseEntity {

    /**
     * Codigo publico de la reserva que el cliente puede presentar al llegar.
     */
    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    /**
     * Placa del vehiculo asociada a la reserva, cuando el cliente la proporcione.
     */
    @Column(name = "placa", length = 15)
    private String placa;

    /**
     * Estado actual del ciclo de vida de la reserva.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoReserva estado = EstadoReserva.CREADA;

    /**
     * Fecha y hora en que se genero la reserva.
     */
    @Column(name = "fecha_reserva", nullable = false)
    private LocalDateTime fechaReserva;

    /**
     * Fecha y hora limite hasta la que la reserva permanece vigente.
     */
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    /**
     * Duracion aplicada a esta reserva en minutos.
     *
     * <p>El valor se guardara en cada reserva para conservar trazabilidad aunque
     * la configuracion cambie posteriormente.</p>
     */
    @Column(name = "tiempo_expiracion_minutos", nullable = false)
    private Integer tiempoExpiracionMinutos;

    /**
     * Cliente que genero la reserva.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "usuario_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reserva_usuario")
    )
    private Usuario usuario;

    /**
     * Estacionamiento donde se realizo la reserva.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "estacionamiento_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reserva_estacionamiento")
    )
    private Estacionamiento estacionamiento;

    /**
     * Cajon apartado por la reserva.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cajon_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reserva_cajon")
    )
    private Cajon cajon;
}
