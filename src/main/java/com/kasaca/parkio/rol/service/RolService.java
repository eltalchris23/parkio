package com.kasaca.parkio.rol.service;

import com.kasaca.parkio.rol.entity.Rol;

import java.util.List;

public interface RolService {

    List<Rol> getRoles();
    Rol getRol(Long rolId);
    Rol addRol(Rol rol);
    Rol updateRol(Long rolId,Rol rol);
    void deleteRol(Long rolId);
}
