#!/usr/bin/env bash
# Restaure une sauvegarde PlanSécu. ATTENTION : remplace toutes les données actuelles.
# Usage (depuis le dossier du projet) : bash deploy/restauration.sh sauvegardes/plansecu-2026-11-10_0300.sql.gz
set -euo pipefail

if [ $# -ne 1 ] || [ ! -f "$1" ]; then
  echo "Usage : $0 <fichier .sql.gz>" >&2
  exit 1
fi

PROJET="$(cd "$(dirname "$0")/.." && pwd)"
FICHIER="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
cd "$PROJET"

read -r -p "Remplacer TOUTES les données actuelles par $FICHIER ? Tapez OUI : " reponse
[ "$reponse" = "OUI" ] || { echo "Annulé."; exit 1; }

echo "Sauvegarde de sécurité de l'état actuel avant restauration…"
bash "$PROJET/deploy/sauvegarde.sh"

docker compose stop app
gzip -dc "$FICHIER" | docker compose exec -T mysql \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" plansecu'
docker compose start app
bash "$PROJET/deploy/connecter-proxy.sh"
echo "Restauration terminée."
