package com.plansecu.repository;

import com.plansecu.domain.Mission;
import com.plansecu.domain.StatutMission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    @EntityGraph(attributePaths = {"agent", "site", "creneaux"})
    List<Mission> findByDateMissionBetweenOrderByDateMissionAsc(LocalDate debut, LocalDate fin);

    @EntityGraph(attributePaths = {"agent", "site", "creneaux"})
    List<Mission> findByAgentIdAndDateMissionBetweenOrderByDateMissionAsc(Long agentId, LocalDate debut, LocalDate fin);

    @EntityGraph(attributePaths = {"agent", "site", "creneaux"})
    @Query("select m from Mission m where m.id = :id")
    Optional<Mission> findDetailById(Long id);

    boolean existsByAgentId(Long agentId);

    boolean existsBySiteId(Long siteId);

    @Modifying
    @Query("update Mission m set m.statut = :nouveau where m.statut = :ancien and m.dateMission < :date")
    int changerStatutAvant(StatutMission ancien, StatutMission nouveau, LocalDate date);
}
