-- Pointage par QR code : un code secret par lieu de mission, et les pointages arrivée / départ.

-- Rempli au démarrage par l'application pour les lieux existants (QrTokenInitializer)
ALTER TABLE site ADD COLUMN qr_token VARCHAR(40);
CREATE UNIQUE INDEX uk_site_qr_token ON site (qr_token);

CREATE TABLE pointage (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    mission_id  BIGINT      NOT NULL,
    type        VARCHAR(10) NOT NULL,          -- ARRIVEE | DEPART
    horodatage  DATETIME    NOT NULL,
    manuel      BOOLEAN     NOT NULL DEFAULT FALSE,  -- saisi par l'admin (oubli de scan)
    saisi_par   VARCHAR(100),
    commentaire VARCHAR(255),
    CONSTRAINT fk_pointage_mission FOREIGN KEY (mission_id) REFERENCES mission (id) ON DELETE CASCADE
);
CREATE INDEX idx_pointage_mission ON pointage (mission_id, horodatage);
