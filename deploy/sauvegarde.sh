#!/usr/bin/env bash
# Sauvegarde de la base PlanSécu (fichier .sql.gz daté), conserve les 30 derniers jours.
# Usage manuel (depuis le dossier du projet) :  bash deploy/sauvegarde.sh
# Automatique (cron, chaque nuit à 3h) : voir DEPLOIEMENT.md
set -euo pipefail

PROJET="$(cd "$(dirname "$0")/.." && pwd)"
DEST="${PROJET}/sauvegardes"
JOURS=30
mkdir -p "$DEST"

FICHIER="$DEST/plansecu-$(date +%Y-%m-%d_%H%M).sql.gz"

cd "$PROJET"
docker compose exec -T mysql \
  sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --no-tablespaces plansecu' \
  | gzip > "$FICHIER"

# Une sauvegarde vide signifie un échec
if [ "$(gzip -dc "$FICHIER" | head -c 100 | wc -c)" -lt 100 ]; then
  echo "ERREUR : sauvegarde vide, vérifier que le conteneur plansecu-mysql tourne." >&2
  rm -f "$FICHIER"
  exit 1
fi

find "$DEST" -name 'plansecu-*.sql.gz' -mtime +"$JOURS" -delete
echo "Sauvegarde OK : $FICHIER ($(du -h "$FICHIER" | cut -f1))"
