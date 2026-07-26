package com.kasaca.parkio.tarifa.entity;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Representa la configuracion de cobro de un estacionamiento.
 *
 * <p>Esta entidad define los valores que se usaran posteriormente
 * para calcular el importe final cuando se cierre un ticket.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tarifa_estacionamiento")
public class TarifaEstacionamiento extends BaseEntity {

    /**
     * Estacionamiento al que pertenece la tarifa.
     *
     * <p>La relacion es uno a uno porque, en esta version, cada estacionamiento
     * debe tener como maximo una tarifa configurada.</p>
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "estacionamiento_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tarifa_estacionamiento_estacionamiento")
    )
    private Estacionamiento estacionamiento;

    /**
     * Precio base que cobra el estacionamiento por hora.
     */
    @Column(
            name = "precio_por_hora",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal precioPorHora;

    /**
     * Minutos de tolerancia antes de comenzar a cobrar.
     *
     * <p>Ejemplo: si la tolerancia es 10 y el cliente sale en 8 minutos,
     * el sistema podria cobrar 0 cuando implementemos el calculo.</p>
     */
    @Column(name = "minutos_tolerancia", nullable = false)
    private Integer minutosTolerancia;

    /**
     * Indica si una fraccion de hora se cobra como hora completa.
     *
     * <p>Por ejemplo, si el cliente estuvo 1 hora y 10 minutos,
     * con este valor en true se cobrarian 2 horas.</p>
     */
    @Column(name = "cobrar_fraccion", nullable = false)
    private Boolean cobrarFraccion;

    /**
     * Monto minimo a cobrar cuando la estancia supera la tolerancia.
     *
     * <p>Sirve para evitar cobros demasiado bajos cuando el cliente estuvo
     * pocos minutos pero ya supero el tiempo gratuito permitido.</p>
     */
    @Column(
            name = "tarifa_minima",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal tarifaMinima;
}
