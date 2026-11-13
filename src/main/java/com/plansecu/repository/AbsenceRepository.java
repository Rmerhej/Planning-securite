package com.plansecu.repository;

import com.plansecu.domain.Absence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    @Query("select a from Absence a join fetch a.agent where a.dateDebut <= :fin and a.dateFin >= :debut order by a.dateDebut")
    List<Absence> chevauchant(LocalDate debut, LocalDate fin);

    @Query("select a from Absence a join fetch a.agent where a.agent.id = :agentId and a.dateDebut <= :fin and a.dateFin >= :debut order by a.dateDebut")
    List<Absence> chevauchantAgent(Long agentId, LocalDate debut, LocalDate fin);
}
