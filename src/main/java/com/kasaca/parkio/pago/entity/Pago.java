package com.kasaca.parkio.pago.entity;

import com.kasaca.parkio.shared.entity.BaseEntity;
import com.kasaca.parkio.ticket.entity.Ticket;
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
 * Representa el pago registrado para liquidar un ticket pendiente de pago.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pago")
public class Pago extends BaseEntity {

    /**
     * Ticket liquidado por este pago.
     *
     * <p>La relacion es uno a uno porque un ticket solo puede tener un pago activo.</p>
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pago_ticket"))
    private Ticket ticket;

    /**
     * Usuario autenticado que registra el cobro en caja.
     *
     * <p>Puede representar a ADMIN, OWNER u OPERADOR segun las reglas de seguridad.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operador_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pago_operador"))
    private Usuario operador;

    /**
     * Monto total calculado previamente en el ticket al registrar la salida.
     */
    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    /**
     * Monto recibido del cliente al momento de registrar el pago.
     */
    @Column(name = "monto_recibido", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoRecibido;

    /**
     * Cambio que debe regresarse al cliente cuando paga mas del monto total.
     */
    @Column(name = "cambio", nullable = false, precision = 10, scale = 2)
    private BigDecimal cambio;

    /**
     * Metodo de pago utilizado por el cliente.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 30)
    private MetodoPago metodoPago;

    /**
     * Estado actual del pago.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPago estado;

    /**
     * Fecha y hora en que el pago fue registrado en el sistema.
     */
    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;
}
