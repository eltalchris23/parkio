package com.kasaca.parkio.estacionamiento.service;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;

import java.util.List;

public interface EstacionamientoService {

    List<Estacionamiento> getEstacionamientos();
    Estacionamiento getEstacionamientoById(Long id);
    Estacionamiento addEstacionamiento(Estacionamiento estacionamiento);
    Estacionamiento updateEstacionamiento(Long id, Estacionamiento estacionamiento);
    void deleteEstacionamiento(Long id);
}
