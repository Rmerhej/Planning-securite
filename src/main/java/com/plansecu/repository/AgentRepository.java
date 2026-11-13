package com.plansecu.repository;

import com.plansecu.domain.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Long> {

    List<Agent> findAllByOrderByNomAscPrenomAsc();

    List<Agent> findByActifTrueOrderByNomAscPrenomAsc();

    Optional<Agent> findByCalendrierToken(String token);

    @Query("select a from Agent a where a.actif = true and a.carteProExpiration is not null "
            + "and a.carteProExpiration <= :limite order by a.carteProExpiration")
    List<Agent> cartesProAExpirer(LocalDate limite);
}
