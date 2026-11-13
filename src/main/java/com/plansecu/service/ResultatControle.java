package com.plansecu.service;

import java.util.List;

/**
 * Résultat du contrôle d'une mission.
 * Les erreurs bloquent l'enregistrement, les avertissements sont seulement affichés.
 */
public record ResultatControle(List<String> erreurs, List<String> avertissements) {

    public boolean valide() {
        return erreurs.isEmpty();
    }
}
