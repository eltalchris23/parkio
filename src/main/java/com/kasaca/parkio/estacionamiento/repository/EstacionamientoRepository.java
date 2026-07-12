package com.kasaca.parkio.estacionamiento.repository;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstacionamientoRepository extends JpaRepository<Estacionamiento, Long> {

    List<Estacionamiento> findByActivoTrue();

    Optional<Estacionamiento> findByIdAndActivoTrue(Long id);
}
