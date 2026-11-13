-- Données d'exemple PlanSécu : 4 agents, 3 lieux, missions sur les 3 prochains jours.
-- Peut être exécuté sur une base qui contient déjà des données (ajoute sans rien effacer).
-- Les comptes de connexion des agents se créent ensuite depuis l'appli (Agents > Modifier > Identifiant).

SET NAMES utf8mb4;

-- ---------------------------------------------------------------- agents
INSERT INTO agent (nom, prenom, telephone, email, carte_pro_numero, carte_pro_expiration, couleur,
                   actif, notif_sms, notif_email, recoit_hebdo, recoit_mensuel, calendrier_token, cree_le)
VALUES ('Dupont', 'Jean', '0612345678', 'jean.dupont@example.com', 'CAR-069-2025-1001',
        DATE_ADD(CURDATE(), INTERVAL 2 YEAR), '#2563eb', TRUE, TRUE, TRUE, TRUE, FALSE, REPLACE(UUID(), '-', ''), NOW());
SET @jean = LAST_INSERT_ID();

INSERT INTO agent (nom, prenom, telephone, email, carte_pro_numero, carte_pro_expiration, couleur,
                   actif, notif_sms, notif_email, recoit_hebdo, recoit_mensuel, calendrier_token, cree_le)
VALUES ('Martin', 'Sophie', '0623456789', 'sophie.martin@example.com', 'CAR-069-2025-1002',
        DATE_ADD(CURDATE(), INTERVAL 1 YEAR), '#db2777', TRUE, TRUE, TRUE, TRUE, TRUE, REPLACE(UUID(), '-', ''), NOW());
SET @sophie = LAST_INSERT_ID();

INSERT INTO agent (nom, prenom, telephone, email, carte_pro_numero, carte_pro_expiration, couleur,
                   actif, notif_sms, notif_email, recoit_hebdo, recoit_mensuel, calendrier_token, cree_le)
VALUES ('Benali', 'Karim', '0634567890', NULL, 'CAR-069-2025-1003',
        DATE_ADD(CURDATE(), INTERVAL 18 MONTH), '#7c3aed', TRUE, TRUE, FALSE, TRUE, FALSE, REPLACE(UUID(), '-', ''), NOW());
SET @karim = LAST_INSERT_ID();

-- Carte pro qui expire dans 20 jours : apparaît dans les alertes du dashboard
INSERT INTO agent (nom, prenom, telephone, email, carte_pro_numero, carte_pro_expiration, couleur,
                   actif, notif_sms, notif_email, recoit_hebdo, recoit_mensuel, calendrier_token, cree_le)
VALUES ('Petit', 'Laura', '0645678901', 'laura.petit@example.com', 'CAR-069-2025-1004',
        DATE_ADD(CURDATE(), INTERVAL 20 DAY), '#059669', TRUE, TRUE, TRUE, TRUE, FALSE, REPLACE(UUID(), '-', ''), NOW());
SET @laura = LAST_INSERT_ID();

-- ---------------------------------------------------------------- lieux
INSERT INTO site (nom, adresse, code_postal, ville, client, contact_nom, contact_telephone, consignes, actif)
VALUES ('Carrefour Market Bellecour', '12 rue de la République', '69002', 'Lyon', 'Carrefour', 'M. Garnier', '0478000001',
        'Ronde parking toutes les heures. Contrôle des sorties caisses.', TRUE);
SET @carrefour = LAST_INSERT_ID();

INSERT INTO site (nom, adresse, code_postal, ville, client, contact_nom, contact_telephone, consignes, actif)
VALUES ('Monoprix Part-Dieu', '17 rue du Docteur Bouchut', '69003', 'Lyon', 'Monoprix', 'Mme Leroy', '0478000002',
        'Poste fixe à l''entrée principale.', TRUE);
SET @monoprix = LAST_INSERT_ID();

INSERT INTO site (nom, adresse, code_postal, ville, client, contact_nom, contact_telephone, consignes, actif)
VALUES ('Entrepôt Lidl Corbas', 'Rue des Frères Lumière', '69960', 'Corbas', 'Lidl', 'Accueil logistique', '0478000004',
        'Gardiennage de nuit, rondes toutes les 2h, main courante.', TRUE);
SET @lidl = LAST_INSERT_ID();

-- ---------------------------------------------------------------- missions
-- Jean : coupure 9h-12h / 15h-18h, aujourd'hui et demain
INSERT INTO mission (date_mission, agent_id, site_id, statut, cree_le, modifie_le)
VALUES (CURDATE(), @jean, @carrefour, 'PLANIFIEE', NOW(), NOW());
SET @m = LAST_INSERT_ID();
INSERT INTO creneau (mission_id, heure_debut, heure_fin) VALUES (@m, '09:00', '12:00'), (@m, '15:00', '18:00');

INSERT INTO mission (date_mission, agent_id, site_id, statut, cree_le, modifie_le)
VALUES (DATE_ADD(CURDATE(), INTERVAL 1 DAY), @jean, @carrefour, 'PLANIFIEE', NOW(), NOW());
SET @m = LAST_INSERT_ID();
INSERT INTO creneau (mission_id, heure_debut, heure_fin) VALUES (@m, '09:00', '12:00'), (@m, '15:00', '18:00');

-- Sophie : journée 8h-16h, aujourd'hui et après-demain
INSERT INTO mission (date_mission, agent_id, site_id, statut, cree_le, modifie_le)
VALUES (CURDATE(), @sophie, @monoprix, 'PLANIFIEE', NOW(), NOW());
SET @m = LAST_INSERT_ID();
INSERT INTO creneau (mission_id, heure_debut, heure_fin) VALUES (@m, '08:00', '16:00');

INSERT INTO mission (date_mission, agent_id, site_id, statut, cree_le, modifie_le)
VALUES (DATE_ADD(CURDATE(), INTERVAL 2 DAY), @sophie, @monoprix, 'PLANIFIEE', NOW(), NOW());
SET @m = LAST_INSERT_ID();
INSERT INTO creneau (mission_id, heure_debut, heure_fin) VALUES (@m, '08:00', '16:00');

-- Karim : nuits 21h-6h, ce soir et demain soir
INSERT INTO mission (date_mission, agent_id, site_id, statut, cree_le, modifie_le)
VALUES (CURDATE(), @karim, @lidl, 'PLANIFIEE', NOW(), NOW());
SET @m = LAST_INSERT_ID();
INSERT INTO creneau (mission_id, heure_debut, heure_fin) VALUES (@m, '21:00', '06:00');

INSERT INTO mission (date_mission, agent_id, site_id, statut, cree_le, modifie_le)
VALUES (DATE_ADD(CURDATE(), INTERVAL 1 DAY), @karim, @lidl, 'PLANIFIEE', NOW(), NOW());
SET @m = LAST_INSERT_ID();
INSERT INTO creneau (mission_id, heure_debut, heure_fin) VALUES (@m, '21:00', '06:00');

-- Laura : coupure tardive 10h-14h / 16h-20h, demain
INSERT INTO mission (date_mission, agent_id, site_id, statut, cree_le, modifie_le)
VALUES (DATE_ADD(CURDATE(), INTERVAL 1 DAY), @laura, @monoprix, 'PLANIFIEE', NOW(), NOW());
SET @m = LAST_INSERT_ID();
INSERT INTO creneau (mission_id, heure_debut, heure_fin) VALUES (@m, '10:00', '14:00'), (@m, '16:00', '20:00');

-- ---------------------------------------------------------------- absence
-- Laura en congés dans une semaine, 3 jours
INSERT INTO absence (agent_id, date_debut, date_fin, type, motif)
VALUES (@laura, DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 9 DAY), 'CONGE', 'Congés payés');
