package com.kasaca.parkio.cajon.entity;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cajon")
public class Cajon extends BaseEntity {

    @Column(name = "numero", length = 20, nullable = false)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30, nullable = false)
    private TipoCajon tipo;

    @Builder.Default // Garantiza que el estado sea LIBRE cuando se use
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30, nullable = false)
    private EstadoCajon estado = EstadoCajon.LIBRE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estacionamiento_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cajon_estacionamiento"))
    private Estacionamiento estacionamiento;
}
