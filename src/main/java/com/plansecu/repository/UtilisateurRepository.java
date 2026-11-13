package com.plansecu.repository;

import com.plansecu.domain.Role;
import com.plansecu.domain.Utilisateur;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    @EntityGraph(attributePaths = "agent")
    Optional<Utilisateur> findByUsername(String username);

    Optional<Utilisateur> findByAgentId(Long agentId);

    @EntityGraph(attributePaths = "agent")
    List<Utilisateur> findByRole(Role role);

    boolean existsByRole(Role role);

    boolean existsByUsername(String username);
}
