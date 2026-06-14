package com.kasaca.parkio.usuario.service;

import com.kasaca.parkio.usuario.entity.Usuario;

import java.util.List;

public class UsuarioServiceImpl implements UsuarioService{
    @Override
    public List<Usuario> getAllUsers() {
        return List.of();
    }

    @Override
    public Usuario getUserById(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Usuario addUser(Usuario usuario) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Usuario updateUser(Long id, Usuario usuario) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deleteUser(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
