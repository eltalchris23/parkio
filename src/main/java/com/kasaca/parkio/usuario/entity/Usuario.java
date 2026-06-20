package com.kasaca.parkio.usuario.entity;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.rol.entity.Rol;
import com.kasaca.parkio.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuario")
public class Usuario extends BaseEntity {

    @Column(
            name = "nombre",
            nullable = false,
            length = 100
    )
    private String nombre;

    @Column(
            name = "apellido",
            length = 100
    )
    private String apellido;

    @Column(
            name = "email",
            nullable = false,
            unique = true,
            length = 150
    )
    private String email;

    @Column(
            name = "password_hash",
            nullable = false
    )
    private String passwordHash;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    @Builder.Default
    private Set<Rol> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_estacionamiento",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "estacionamiento_id")
    )
    @Builder.Default
    private Set<Estacionamiento> estacionamientos = new HashSet<>();
}
