package com.kasaca.parkio.estacionamiento.service;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;

import java.util.List;

public class EstacionamientoServiceImpl implements EstacionamientoService {
    @Override
    public List<Estacionamiento> getEstacionamientos() {
        return List.of();
    }

    @Override
    public Estacionamiento getEstacionamientoById(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Estacionamiento addEstacionamiento(Estacionamiento estacionamiento) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Estacionamiento updateEstacionamiento(Long id, Estacionamiento estacionamiento) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deleteEstacionamiento(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
