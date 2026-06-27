package com.kasaca.parkio.cajon.repository;

import com.kasaca.parkio.cajon.entity.Cajon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CajonRepository extends JpaRepository<Cajon, Long> {

    // Detectar duplicados al crear
    boolean existsByEstacionamientoIdAndNumero(
            Long estacionamientoId,
            String numero
    );

    // Detectar duplicados al actualizar, excluyendo el mismo cajon
    boolean existsByEstacionamientoIdAndNumeroAndIdNot(
            Long estacionamientoId,
            String numero,
            Long cajonId
    );

    List<Cajon> findByEstacionamientoId(Long estacionamientoId);
}
