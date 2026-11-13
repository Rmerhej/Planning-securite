package com.plansecu.service;

import com.plansecu.domain.Mission;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @param missions        missions enregistrées
 * @param erreursParDate  dates refusées et raisons (vide si tout est passé)
 * @param avertissements  avertissements non bloquants
 */
public record ResultatEnregistrement(List<Mission> missions,
                                     Map<LocalDate, List<String>> erreursParDate,
                                     List<String> avertissements) {

    public boolean aucuneEnregistree() {
        return missions.isEmpty();
    }

    public List<String> toutesLesErreurs() {
        return erreursParDate.values().stream().flatMap(List::stream).distinct().toList();
    }
}
