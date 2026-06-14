package com.kasaca.parkio.cajon.service;

import com.kasaca.parkio.cajon.entity.Cajon;

import java.util.List;

public class CajonServiceImpl implements CajonService {
    @Override
    public List<Cajon> getCajones() {
        return List.of();
    }

    @Override
    public Cajon getCajon(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Cajon addCajon(Cajon cajon) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Cajon updateCajon(Long id, Cajon cajon) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deleteCajon(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
