package com.kasaca.parkio.cajon.repository;

import com.kasaca.parkio.cajon.entity.Cajon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

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

    List<Cajon> findByActivoTrue();

    Optional<Cajon> findByIdAndActivoTrue(Long id);

    List<Cajon> findByEstacionamientoIdAndActivoTrue(Long estacionamientoId);
}
