#!/usr/bin/env bash
# Branche les conteneurs PlanSécu (plansecu-app, et plansecu-demo s'il existe) sur le réseau Docker
# du proxy (Caddy, Nginx Proxy Manager…) quand celui-ci tourne dans Docker.
# Ne fait rien si PROXY_NETWORK est vide dans .env.
#
# Pourquoi pas dans docker-compose ? Compose ajouterait aussi les noms de service courts (« app »)
# sur ce réseau : si une autre appli du serveur a elle aussi un service « app », le proxy confondrait
# les deux. Avec « docker network connect », seul le nom complet du conteneur est visible.
#
# Usage (depuis le dossier du projet) : bash deploy/connecter-proxy.sh
set -euo pipefail

PROJET="$(cd "$(dirname "$0")/.." && pwd)"
RESEAU="$(grep -E '^PROXY_NETWORK=' "$PROJET/.env" 2>/dev/null | cut -d= -f2- | tr -d '"' | tr -d "'" || true)"

[ -z "$RESEAU" ] && exit 0

for conteneur in plansecu-app plansecu-demo; do
  # plansecu-demo n'existe que si la démo est activée
  docker inspect "$conteneur" >/dev/null 2>&1 || continue
  if docker inspect "$conteneur" --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}' | grep -qw "$RESEAU"; then
    echo "$conteneur est déjà sur le réseau $RESEAU"
  else
    docker network connect "$RESEAU" "$conteneur"
    echo "$conteneur branché sur le réseau $RESEAU"
  fi
done
