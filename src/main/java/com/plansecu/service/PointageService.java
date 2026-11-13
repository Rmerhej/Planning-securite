package com.plansecu.service;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.*;
import com.plansecu.repository.MissionRepository;
import com.plansecu.repository.PointageRepository;
import com.plansecu.repository.SiteRepository;
import com.plansecu.repository.UtilisateurRepository;
import com.plansecu.service.CalculHeures.Plage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pointage par QR code : l'agent scanne le code affiché sur le lieu, l'appli retrouve
 * sa mission en cours et enregistre l'arrivée ou le départ.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PointageService {

    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH'h'mm");
    /** Délai après la fin de mission pendant lequel on peut encore pointer son départ. */
    private static final Duration MARGE_DEPART = Duration.ofHours(2);
    /** Après ce délai sans départ pointé, la mission est signalée. */
    private static final Duration ALERTE_SANS_DEPART = Duration.ofMinutes(30);
    /** Deux scans à moins d'une minute d'écart = double appui, ignoré. */
    private static final Duration ANTI_DOUBLON = Duration.ofMinutes(1);

    private final SiteRepository siteRepository;
    private final MissionRepository missionRepository;
    private final PointageRepository pointageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AppProperties props;
    private final Clock clock;

    /** État présenté à l'agent après le scan. */
    public record EtatPointage(boolean possible,
                               String message,
                               Site site,
                               Mission mission,
                               List<Pointage> pointages,
                               TypePointage prochain,
                               Pointage dernier) {

        static EtatPointage refus(Site site, String message) {
            return new EtatPointage(false, message, site, null, List.of(), null, null);
        }
    }

    // ------------------------------------------------------------ côté agent

    /** Ce que verra l'agent en scannant le QR code (sans rien enregistrer). */
    @Transactional(readOnly = true)
    public EtatPointage preparer(String token, String username) {
        return etat(token, username, LocalDateTime.now(clock), null);
    }

    /**
     * Enregistre le pointage attendu. {@code type} est celui affiché à l'agent : s'il ne correspond plus
     * (double envoi du formulaire), rien n'est enregistré.
     */
    public EtatPointage pointer(String token, String username, TypePointage type) {
        LocalDateTime maintenant = LocalDateTime.now(clock).withNano(0);
        EtatPointage etat = etat(token, username, maintenant, null);
        if (!etat.possible()) return etat;

        Pointage dernier = etat.dernier();
        boolean doublon = dernier != null && Duration.between(dernier.getHorodatage(), maintenant).compareTo(ANTI_DOUBLON) < 0;
        if (type != etat.prochain() || doublon) {
            return etat(token, username, maintenant, "Pointage déjà enregistré.");
        }

        Pointage p = pointageRepository.save(new Pointage(etat.mission(), type, maintenant));
        String message = (type == TypePointage.ARRIVEE ? "Arrivée" : "Départ") + " enregistré(e) à " + HEURE.format(maintenant) + ".";
        if (type == TypePointage.ARRIVEE) {
            long retard = retardAuCreneau(etat.mission(), maintenant);
            if (retard > props.getRegles().getToleranceRetardMinutes()) {
                message += " Retard de " + retard + " min.";
            }
        }
        List<Pointage> pointages = new ArrayList<>(etat.pointages());
        pointages.add(p);
        return new EtatPointage(true, message, etat.site(), etat.mission(), pointages, suivant(type), p);
    }

    private EtatPointage etat(String token, String username, LocalDateTime maintenant, String message) {
        Site site = siteRepository.findByQrToken(token).orElse(null);
        if (site == null || !site.isActif()) {
            return EtatPointage.refus(null, "Ce QR code n'est plus valide. Prévenez votre responsable.");
        }
        Agent agent = utilisateurRepository.findByUsername(username).map(Utilisateur::getAgent).orElse(null);
        if (agent == null) {
            return EtatPointage.refus(site, "Votre compte n'est pas rattaché à un agent.");
        }

        List<Mission> candidates = missionRepository
                .findByAgentIdAndDateMissionBetweenOrderByDateMissionAsc(agent.getId(), maintenant.toLocalDate().minusDays(1),
                        maintenant.toLocalDate())
                .stream()
                .filter(Mission::occupeAgent)
                .filter(m -> fenetre(m).contains(maintenant))
                .toList();

        Optional<Mission> ici = candidates.stream()
                .filter(m -> m.getSite().getId().equals(site.getId()))
                .min(Comparator.comparing(m -> Duration.between(debut(m), maintenant).abs()));

        if (ici.isEmpty()) {
            String ailleurs = candidates.stream().map(m -> "« " + m.getSite().getNom() + " » (" + m.getHorairesLibelle() + ")")
                    .collect(Collectors.joining(", "));
            return EtatPointage.refus(site, "Aucune mission prévue pour vous ici en ce moment."
                    + (ailleurs.isEmpty() ? "" : " Vous êtes planifié(e) sur " + ailleurs + "."));
        }

        Mission mission = ici.get();
        List<Pointage> pointages = pointageRepository.findByMissionIdOrderByHorodatageAsc(mission.getId());
        Pointage dernier = pointages.isEmpty() ? null : pointages.get(pointages.size() - 1);
        TypePointage prochain = dernier == null ? TypePointage.ARRIVEE : suivant(dernier.getType());
        return new EtatPointage(true, message, site, mission, pointages, prochain, dernier);
    }

    private static TypePointage suivant(TypePointage t) {
        return t == TypePointage.ARRIVEE ? TypePointage.DEPART : TypePointage.ARRIVEE;
    }

    private record Fenetre(LocalDateTime debut, LocalDateTime fin) {
        boolean contains(LocalDateTime t) {
            return !t.isBefore(debut) && !t.isAfter(fin);
        }
    }

    private Fenetre fenetre(Mission m) {
        List<Plage> plages = CalculHeures.plages(m);
        LocalDateTime d = plages.stream().map(Plage::debut).min(Comparator.naturalOrder()).orElseThrow();
        LocalDateTime f = plages.stream().map(Plage::fin).max(Comparator.naturalOrder()).orElseThrow();
        return new Fenetre(d.minusMinutes(props.getRegles().getPointageAvanceMinutes()), f.plus(MARGE_DEPART));
    }

    private static LocalDateTime debut(Mission m) {
        return CalculHeures.plages(m).stream().map(Plage::debut).min(Comparator.naturalOrder()).orElseThrow();
    }

    /** Retard par rapport au créneau en cours ou au prochain créneau. */
    private long retardAuCreneau(Mission m, LocalDateTime arrivee) {
        return CalculHeures.plages(m).stream()
                .filter(p -> arrivee.isBefore(p.fin()))
                .min(Comparator.comparing(Plage::debut))
                .map(p -> Math.max(0, Duration.between(p.debut(), arrivee).toMinutes()))
                .orElse(0L);
    }

    // ------------------------------------------------------------ côté admin

    @Transactional(readOnly = true)
    public List<SuiviPointage> suivre(List<Mission> missions) {
        if (missions.isEmpty()) return List.of();
        Map<Long, List<Pointage>> parMission = pointageRepository
                .deMissions(missions.stream().map(Mission::getId).toList()).stream()
                .collect(Collectors.groupingBy(p -> p.getMission().getId()));
        LocalDateTime maintenant = LocalDateTime.now(clock);
        return missions.stream()
                .map(m -> suivi(m, parMission.getOrDefault(m.getId(), List.of()), maintenant))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<Pointage>> pointagesParMission(Collection<Long> missionIds) {
        if (missionIds.isEmpty()) return Map.of();
        return pointageRepository.deMissions(missionIds).stream()
                .collect(Collectors.groupingBy(p -> p.getMission().getId()));
    }

    public SuiviPointage suivi(Mission m, List<Pointage> pointages, LocalDateTime maintenant) {
        List<Pointage> tries = pointages.stream().sorted(Comparator.comparing(Pointage::getHorodatage)).toList();
        long minutes = minutesPointees(tries);
        if (!m.occupeAgent()) {
            return new SuiviPointage(m, tries, minutes, 0, SuiviPointage.Etat.SANS_OBJET, List.of());
        }

        int tolerance = props.getRegles().getToleranceRetardMinutes();
        int avance = props.getRegles().getPointageAvanceMinutes();
        List<Plage> plages = CalculHeures.plages(m).stream().sorted(Comparator.comparing(Plage::debut)).toList();
        List<LocalDateTime> arrivees = tries.stream().filter(p -> p.getType() == TypePointage.ARRIVEE)
                .map(Pointage::getHorodatage).collect(Collectors.toCollection(ArrayList::new));

        List<String> anomalies = new ArrayList<>();
        long retardTotal = 0;
        for (Plage plage : plages) {
            Optional<LocalDateTime> arrivee = arrivees.stream()
                    .filter(a -> !a.isBefore(plage.debut().minusMinutes(avance)) && a.isBefore(plage.fin()))
                    .findFirst();
            String creneau = HEURE.format(plage.debut());
            if (arrivee.isPresent()) {
                arrivees.remove(arrivee.get());
                long retard = Duration.between(plage.debut(), arrivee.get()).toMinutes();
                if (retard > tolerance) {
                    retardTotal += retard;
                    anomalies.add("Retard de " + retard + " min (créneau de " + creneau + ")");
                }
            } else if (maintenant.isAfter(plage.debut().plusMinutes(tolerance))) {
                anomalies.add("Pas d'arrivée pointée (créneau de " + creneau + ")");
            }
        }

        LocalDateTime premierDebut = plages.get(0).debut();
        LocalDateTime derniereFin = plages.get(plages.size() - 1).fin();
        Pointage dernier = tries.isEmpty() ? null : tries.get(tries.size() - 1);
        boolean enPoste = dernier != null && dernier.getType() == TypePointage.ARRIVEE;

        if (enPoste && maintenant.isAfter(derniereFin.plus(ALERTE_SANS_DEPART))) {
            anomalies.add("Départ non pointé");
        }
        if (dernier != null && dernier.getType() == TypePointage.DEPART) {
            long avanceDepart = Duration.between(dernier.getHorodatage(), derniereFin).toMinutes();
            if (avanceDepart > tolerance) anomalies.add("Départ anticipé de " + avanceDepart + " min");
        }

        SuiviPointage.Etat etat;
        if (tries.isEmpty()) {
            etat = maintenant.isAfter(premierDebut.plusMinutes(tolerance)) ? SuiviPointage.Etat.NON_POINTEE : SuiviPointage.Etat.A_VENIR;
        } else if (!anomalies.isEmpty()) {
            etat = SuiviPointage.Etat.ANOMALIE;
        } else if (enPoste || maintenant.isBefore(derniereFin)) {
            etat = SuiviPointage.Etat.EN_COURS;
        } else {
            etat = SuiviPointage.Etat.TERMINEE;
        }
        return new SuiviPointage(m, tries, minutes, retardTotal, etat, anomalies);
    }

    /** Somme des durées arrivée → départ suivant. Une arrivée sans départ ne compte pas. */
    public static long minutesPointees(List<Pointage> tries) {
        long total = 0;
        LocalDateTime ouverture = null;
        for (Pointage p : tries) {
            if (p.getType() == TypePointage.ARRIVEE) {
                if (ouverture == null) ouverture = p.getHorodatage();
            } else if (ouverture != null) {
                total += Duration.between(ouverture, p.getHorodatage()).toMinutes();
                ouverture = null;
            }
        }
        return total;
    }

    /** Saisie manuelle par l'admin (agent qui a oublié de scanner, téléphone déchargé…). */
    public Pointage ajouterManuel(Long missionId, TypePointage type, LocalDateTime horodatage, String admin, String commentaire) {
        Mission m = missionRepository.findById(missionId).orElseThrow();
        Pointage p = new Pointage(m, type, horodatage.withSecond(0).withNano(0));
        p.setManuel(true);
        p.setSaisiPar(admin);
        p.setCommentaire(commentaire == null || commentaire.isBlank() ? null : commentaire.trim());
        return pointageRepository.save(p);
    }

    /** @return la date de la mission concernée */
    public LocalDate supprimer(Long pointageId) {
        Pointage p = pointageRepository.findById(pointageId).orElseThrow();
        LocalDate date = p.getMission().getDateMission();
        pointageRepository.delete(p);
        return date;
    }
}
