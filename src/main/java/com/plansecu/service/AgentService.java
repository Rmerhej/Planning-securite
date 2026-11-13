package com.plansecu.service;

import com.plansecu.domain.Agent;
import com.plansecu.domain.Role;
import com.plansecu.domain.Utilisateur;
import com.plansecu.repository.AgentRepository;
import com.plansecu.repository.MissionRepository;
import com.plansecu.repository.UtilisateurRepository;
import com.plansecu.web.form.AgentForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AgentService {

    private final AgentRepository agentRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final MissionRepository missionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Optional<String> identifiant(Long agentId) {
        return utilisateurRepository.findByAgentId(agentId).map(Utilisateur::getUsername);
    }

    /** Vérifie les règles du compte de connexion. Renvoie un message d'erreur, ou null si OK. */
    @Transactional(readOnly = true)
    public String verifierCompte(AgentForm form) {
        String id = form.getIdentifiant() == null ? "" : form.getIdentifiant().trim();
        if (id.isEmpty()) return null;
        Optional<Utilisateur> existant = form.getId() == null ? Optional.empty() : utilisateurRepository.findByAgentId(form.getId());
        Optional<Utilisateur> homonyme = utilisateurRepository.findByUsername(id);
        if (homonyme.isPresent() && (existant.isEmpty() || !homonyme.get().getId().equals(existant.get().getId()))) {
            return "Cet identifiant est déjà utilisé.";
        }
        boolean mdpVide = form.getMotDePasse() == null || form.getMotDePasse().isBlank();
        if (existant.isEmpty() && mdpVide) {
            return "Saisissez un mot de passe pour créer le compte.";
        }
        if (!mdpVide && form.getMotDePasse().length() < 8) {
            return "Le mot de passe doit faire au moins 8 caractères.";
        }
        return null;
    }

    public Agent enregistrer(AgentForm form) {
        Agent agent = form.getId() == null ? new Agent() : agentRepository.findById(form.getId()).orElseThrow();
        form.appliquer(agent);
        agent = agentRepository.save(agent);

        String id = form.getIdentifiant() == null ? "" : form.getIdentifiant().trim();
        if (!id.isEmpty()) {
            Agent finalAgent = agent;
            Utilisateur u = utilisateurRepository.findByAgentId(agent.getId()).orElseGet(() -> {
                Utilisateur n = new Utilisateur();
                n.setRole(Role.AGENT);
                n.setAgent(finalAgent);
                return n;
            });
            u.setUsername(id);
            u.setActif(agent.isActif());
            if (form.getMotDePasse() != null && !form.getMotDePasse().isBlank()) {
                u.setPassword(passwordEncoder.encode(form.getMotDePasse()));
            }
            utilisateurRepository.save(u);
        } else {
            utilisateurRepository.findByAgentId(agent.getId()).ifPresent(u -> {
                u.setActif(false);
            });
        }
        return agent;
    }

    /** Supprime l'agent s'il n'a aucune mission, sinon le désactive. @return true si supprimé */
    public boolean supprimerOuDesactiver(Long id) {
        Agent agent = agentRepository.findById(id).orElseThrow();
        if (missionRepository.existsByAgentId(id)) {
            agent.setActif(false);
            utilisateurRepository.findByAgentId(id).ifPresent(u -> u.setActif(false));
            return false;
        }
        utilisateurRepository.findByAgentId(id).ifPresent(utilisateurRepository::delete);
        agentRepository.delete(agent);
        return true;
    }

    public void regenererLienCalendrier(Long id) {
        agentRepository.findById(id).orElseThrow().regenererToken();
    }
}
