package com.kasaca.parkio.tarifa.repository;

import com.kasaca.parkio.tarifa.entity.TarifaEstacionamiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la configuracion de tarifas por estacionamiento.
 */
public interface TarifaEstacionamientoRepository extends JpaRepository<TarifaEstacionamiento, Long> {

    /**
     * Busca una tarifa activa por su identificador interno.
     *
     * @param id identificador de la tarifa
     * @return tarifa activa cuando existe
     */
    Optional<TarifaEstacionamiento> findByIdAndActivoTrue(Long id);

    /**
     * Busca la tarifa activa asociada a un estacionamiento especifico.
     *
     * @param estacionamientoId identificador del estacionamiento
     * @return tarifa activa del estacionamiento cuando existe
     */
    Optional<TarifaEstacionamiento> findByEstacionamientoIdAndActivoTrue(Long estacionamientoId);

    /**
     * Verifica si un estacionamiento ya tiene una tarifa activa registrada.
     *
     * @param estacionamientoId identificador del estacionamiento
     * @return {@code true} cuando ya existe una tarifa activa para el estacionamiento
     */
    boolean existsByEstacionamientoIdAndActivoTrue(Long estacionamientoId);
}
