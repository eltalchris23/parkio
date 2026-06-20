package com.kasaca.parkio.estacionamiento.entity;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.shared.entity.BaseEntity;
import com.kasaca.parkio.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "estacionamiento")
public class Estacionamiento extends BaseEntity{

    @Column(
            name = "nombre",
            nullable = false,
            length = 150
    )
    private String nombre;

    @Column(
            name = "descripcion",
            length = 500
    )
    private String descripcion;

    @Column(
            name = "latitud",
            precision = 10,
            scale = 8,
            nullable = false
    )
    private BigDecimal latitud;

    @Column(
            name = "longitud",
            precision = 11,
            scale = 8,
            nullable = false
    )
    private BigDecimal longitud;

    @OneToMany(
            mappedBy = "estacionamiento",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<Cajon> cajones = new HashSet<>();

    @ManyToMany(mappedBy = "estacionamientos")
    @Builder.Default
    private Set<Usuario> usuarios = new HashSet<>();
}
