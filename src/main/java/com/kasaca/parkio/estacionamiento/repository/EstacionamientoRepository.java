package com.kasaca.parkio.estacionamiento.repository;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstacionamientoRepository extends JpaRepository<Estacionamiento, Long> {

    Page<Estacionamiento> findByActivoTrue(Pageable pageable);

    Optional<Estacionamiento> findByIdAndActivoTrue(Long id);

    Page<Estacionamiento> findByOwnerIdAndActivoTrue(Long ownerId, Pageable pageable);

    Optional<Estacionamiento> findByIdAndOwnerIdAndActivoTrue(Long id, Long ownerId);
}
