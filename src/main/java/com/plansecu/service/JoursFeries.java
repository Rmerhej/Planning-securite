package com.plansecu.service;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Jours fériés en France métropolitaine (calcul de Pâques inclus). */
public final class JoursFeries {

    private static final Map<Integer, Set<LocalDate>> CACHE = new ConcurrentHashMap<>();

    private JoursFeries() {}

    public static boolean estFerie(LocalDate date) {
        return feries(date.getYear()).contains(date);
    }

    public static Set<LocalDate> feries(int annee) {
        return CACHE.computeIfAbsent(annee, JoursFeries::calculer);
    }

    private static Set<LocalDate> calculer(int annee) {
        LocalDate paques = paques(annee);
        return Set.of(
                LocalDate.of(annee, Month.JANUARY, 1),
                paques.plusDays(1),                       // lundi de Pâques
                LocalDate.of(annee, Month.MAY, 1),
                LocalDate.of(annee, Month.MAY, 8),
                paques.plusDays(39),                      // Ascension
                paques.plusDays(50),                      // lundi de Pentecôte
                LocalDate.of(annee, Month.JULY, 14),
                LocalDate.of(annee, Month.AUGUST, 15),
                LocalDate.of(annee, Month.NOVEMBER, 1),
                LocalDate.of(annee, Month.NOVEMBER, 11),
                LocalDate.of(annee, Month.DECEMBER, 25));
    }

    /** Dimanche de Pâques (algorithme de Meeus/Jones/Butcher). */
    static LocalDate paques(int y) {
        int a = y % 19, b = y / 100, c = y % 100, d = b / 4, e = b % 4;
        int f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4, k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mois = (h + l - 7 * m + 114) / 31;
        int jour = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(y, mois, jour);
    }
}
