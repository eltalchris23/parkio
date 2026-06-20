package com.kasaca.parkio.estacionamiento.repository;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstacionamientoRepository extends JpaRepository<Estacionamiento, Long> {
}
