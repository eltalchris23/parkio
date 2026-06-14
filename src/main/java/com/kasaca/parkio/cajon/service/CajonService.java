package com.kasaca.parkio.cajon.service;

import com.kasaca.parkio.cajon.entity.Cajon;

import java.util.List;

public interface CajonService {

    List<Cajon> getCajones();
    Cajon getCajon(Long id);
    Cajon addCajon(Cajon cajon);
    Cajon updateCajon(Long id, Cajon cajon);
    void deleteCajon(Long id);
}
