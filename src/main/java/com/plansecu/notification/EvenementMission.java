package com.plansecu.notification;

import java.util.List;

/**
 * Événement publié par le PlanningService. Les notifications partent après validation
 * de la transaction (voir NotificationListener).
 *
 * @param missions missions concernées (même agent)
 * @param avant    état précédent (pour une modification), sinon null
 */
public record EvenementMission(Type type, List<MissionResume> missions, MissionResume avant) {

    public enum Type { CREEE, MODIFIEE, ANNULEE }

    public static EvenementMission creees(List<MissionResume> missions) {
        return new EvenementMission(Type.CREEE, missions, null);
    }

    public static EvenementMission modifiee(MissionResume avant, MissionResume apres) {
        return new EvenementMission(Type.MODIFIEE, List.of(apres), avant);
    }

    public static EvenementMission annulee(MissionResume mission) {
        return new EvenementMission(Type.ANNULEE, List.of(mission), null);
    }
}
