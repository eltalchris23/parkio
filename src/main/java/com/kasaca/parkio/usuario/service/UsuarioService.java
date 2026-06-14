package com.kasaca.parkio.usuario.service;

import com.kasaca.parkio.usuario.entity.Usuario;

import java.util.List;

public interface UsuarioService {

    List<Usuario> getAllUsers();
    Usuario getUserById(Long id);
    Usuario addUser(Usuario usuario);
    Usuario updateUser(Long id, Usuario usuario);
    void deleteUser(Long id);
}
