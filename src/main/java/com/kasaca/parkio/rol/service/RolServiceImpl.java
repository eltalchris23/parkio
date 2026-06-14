package com.kasaca.parkio.rol.service;

import com.kasaca.parkio.rol.entity.Rol;

import java.util.List;

public class RolServiceImpl implements RolService {
    @Override
    public List<Rol> getRoles() {
        return List.of();
    }

    @Override
    public Rol getRol(Long rolId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Rol addRol(Rol rol) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Rol updateRol(Long rolId, Rol rol) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deleteRol(Long rolId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
