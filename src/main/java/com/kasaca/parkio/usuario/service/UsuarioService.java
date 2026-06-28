package com.kasaca.parkio.usuario.service;

import com.kasaca.parkio.usuario.dto.UsuarioRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;

import java.util.List;

public interface UsuarioService {

    List<UsuarioResponse> getAllUsers();

    UsuarioResponse getUserById(Long id);

    UsuarioResponse addUser(UsuarioRequest request);

    UsuarioResponse updateUser(Long id, UsuarioRequest request);

    void deleteUser(Long id);
}
