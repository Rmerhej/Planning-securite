#!/usr/bin/env bash
# Met à jour PlanSécu sur le serveur après avoir envoyé les nouveaux fichiers.
# Fait une sauvegarde, reconstruit l'appli, redémarre, affiche les logs de démarrage.
# Usage (depuis le dossier du projet) : bash deploy/mise-a-jour.sh
set -euo pipefail

PROJET="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJET"

echo "1/3 Sauvegarde de la base…"
bash "$PROJET/deploy/sauvegarde.sh"

echo "2/3 Reconstruction de l'appli (quelques minutes)…"
docker compose build app

echo "3/3 Redémarrage…"
docker compose up -d
bash "$PROJET/deploy/connecter-proxy.sh"

echo "Démarrage en cours, logs (Ctrl+C pour quitter, l'appli continue de tourner) :"
docker compose logs -f --tail=50 app
