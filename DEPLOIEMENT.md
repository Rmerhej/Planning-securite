# Mettre PlanSécu en production sur ton serveur Oracle

Ce guide suppose un serveur Linux (Ubuntu) où tournent déjà 2 applis, avec Docker installé.
Toutes les commandes sont à copier-coller telles quelles. Les parties `EN_MAJUSCULES` sont à remplacer.

- Commandes marquées **PC** : à taper dans PowerShell sur ton PC Windows.
- Commandes marquées **Serveur** : à taper dans la session SSH sur le serveur.

Remplace partout :
- `IP_DU_SERVEUR` par l'adresse IP publique du serveur ;
- `ubuntu` par ton nom d'utilisateur SSH s'il est différent (sur Oracle Linux, c'est souvent `opc`) ;
- `planning.ton-domaine.fr` par le sous-domaine choisi pour PlanSécu.

---

## Étape 0 — Vérifier que le serveur a la place (5 min)

**Serveur** — connecte-toi comme d'habitude, puis :

```bash
free -h
docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}\t{{.CPUPerc}}"
```

- Dans `free -h`, regarde la colonne **available** de la ligne `Mem`.
  PlanSécu a besoin d'environ **700 Mo** (512 Mo maxi pour l'appli, 512 Mo maxi pour MySQL, en pratique ~700 Mo à eux deux).
  - plus de 1,5 Go disponible : aucun souci ;
  - entre 1 et 1,5 Go : ça passe ;
  - moins de 1 Go : envoie-moi le résultat des 2 commandes, on regardera ce qui consomme.
- Dans `docker stats`, si une appli est à 90 % de CPU en permanence, dis-le-moi aussi.

Puis repère ton proxy (ce qui reçoit les visites sur les ports 80/443) :

```bash
sudo ss -tlnp | grep -E ':(80|443)\s'
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}"
```

- Si la première commande montre **nginx** ou **caddy** (sans « docker-proxy ») → tu es dans le **cas A**.
- Si elle montre **docker-proxy**, et que la seconde liste un conteneur comme `nginx-proxy-manager`, `jc21/nginx-proxy-manager`,
  `traefik`, `caddy` ou `nginx` avec `0.0.0.0:443->…` → tu es dans le **cas B**.
- Si rien n'écoute sur 80/443 → **cas C**.

Note le cas, on s'en sert à l'étape 4.

## Étape 1 — Créer le sous-domaine (5 min, puis jusqu'à 1 h d'attente)

Chez le gestionnaire de ton nom de domaine (OVH, Cloudflare, Gandi…), dans la zone DNS, ajoute :

| Type | Nom | Valeur |
|---|---|---|
| A | `planning` | `IP_DU_SERVEUR` |

(Si ton domaine est chez Cloudflare, mets le nuage en **gris** « DNS only » le temps d'obtenir le certificat à l'étape 4.)

Pour vérifier que c'est pris en compte, **PC** :

```powershell
Resolve-DnsName planning.ton-domaine.fr
```

Il doit afficher l'IP du serveur. Sinon, attends un peu et réessaie.

## Étape 2 — Envoyer l'appli sur le serveur (5 min)

**PC** (le zip est dans Téléchargements) :

```powershell
scp "$env:USERPROFILE\Downloads\planning-securite.zip" ubuntu@IP_DU_SERVEUR:~/
```

(Si tu te connectes avec un fichier de clé, ajoute `-i C:\chemin\vers\ta-cle.key` juste après `scp`, comme pour `ssh`.)

**Serveur** :

```bash
sudo apt-get install -y unzip
cd ~
unzip -o planning-securite.zip
cd ~/planning-securite
chmod +x deploy/*.sh
```

L'appli est maintenant dans `~/planning-securite` sur le serveur.

## Étape 3 — Configurer et lancer (10 min)

**Serveur** :

```bash
cd ~/planning-securite
cp .env.example .env
chmod 600 .env
```

Génère 3 mots de passe solides (copie-les quelque part de sûr, ex. un gestionnaire de mots de passe) :

```bash
openssl rand -base64 24; openssl rand -base64 24; openssl rand -base64 24
```

Ouvre le fichier de configuration :

```bash
nano .env
```

Modifie :
- `APP_BASE_URL=https://planning.ton-domaine.fr`
- `DB_PASSWORD=` → 1er mot de passe
- `MYSQL_ROOT_PASSWORD=` → 2e mot de passe
- `ADMIN_PASSWORD=` → 3e mot de passe (c'est celui pour te connecter en admin)
- `APP_PORT=8081` : vérifie que le port est libre avec `sudo ss -tlnp | grep :8081` (rien ne doit s'afficher), sinon mets 8082, 8083…

Enregistre avec **Ctrl+O** puis **Entrée**, quitte avec **Ctrl+X**.

> ⚠ Le mot de passe admin n'est utilisé qu'au **tout premier démarrage**. Le changer ensuite dans `.env` ne fait rien :
> passe par « Mon compte » dans l'appli.

Lance (la première fois, la construction prend 5 à 10 minutes) :

```bash
docker compose up -d --build
docker compose logs -f app
```

Attends la ligne `Started PlanSecuApplication`, puis **Ctrl+C** (l'appli continue de tourner).
Tu dois aussi voir plus haut `Successfully validated 2 migrations`.

Test en local sur le serveur :

```bash
curl -I http://127.0.0.1:8081/login
```

Doit afficher `HTTP/1.1 200`.

## Étape 4 — Brancher le proxy et le HTTPS (10 min)

### Cas A — nginx ou Caddy installé sur le serveur

**nginx :**

```bash
cd ~/planning-securite
sed -i 's/planning.ton-domaine.fr/planning.TON-VRAI-DOMAINE.fr/g' deploy/nginx-plansecu.conf
sudo cp deploy/nginx-plansecu.conf /etc/nginx/sites-available/plansecu
sudo ln -s /etc/nginx/sites-available/plansecu /etc/nginx/sites-enabled/plansecu
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d planning.TON-VRAI-DOMAINE.fr
```

(Si tu as mis un autre `APP_PORT` que 8081, remplace aussi `8081` dans `deploy/nginx-plansecu.conf` avant de le copier.
`nginx -t` doit dire `syntax is ok`. Si `certbot` n'existe pas : `sudo apt-get install -y certbot python3-certbot-nginx`.)

**Caddy :** ajoute le contenu de `deploy/Caddyfile-plansecu` (avec ton domaine) à la fin de `/etc/caddy/Caddyfile` :

```bash
sudo nano /etc/caddy/Caddyfile
sudo systemctl reload caddy
```

Caddy obtient le certificat HTTPS tout seul.

### Cas B — le proxy tourne dans Docker (Nginx Proxy Manager, Traefik…)

1. Trouve le réseau Docker du proxy :

   ```bash
   docker inspect NOM_DU_CONTENEUR_PROXY --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}'
   ```

2. Dans `.env` (`nano .env`), mets :

   ```
   COMPOSE_FILE=docker-compose.prod.yml:deploy/docker-compose.proxy-docker.yml
   PROXY_NETWORK=LE_NOM_AFFICHÉ
   ```

3. Relance : `docker compose up -d`

4. Dans l'interface de ton proxy, ajoute l'hôte `planning.ton-domaine.fr` qui redirige vers
   **`plansecu-app`**, port **`8080`**, en **http**, avec certificat SSL (Let's Encrypt) et « Force SSL ».
   Dans Nginx Proxy Manager : *Hosts → Proxy Hosts → Add Proxy Host*, onglet *SSL* → *Request a new certificate*.

### Cas C — aucun proxy

Installe Caddy (HTTPS automatique) :

```bash
sudo apt-get install -y caddy
sudo cp ~/planning-securite/deploy/Caddyfile-plansecu /etc/caddy/Caddyfile
sudo nano /etc/caddy/Caddyfile        # remplacer le domaine
sudo systemctl reload caddy
```

Sur Oracle, ouvre aussi les ports 80 et 443 : console Oracle Cloud → *Networking → Virtual Cloud Networks → ton VCN →
Security Lists → Add Ingress Rules* (source `0.0.0.0/0`, ports `80` et `443`), puis sur le serveur :

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

### Vérification

Ouvre `https://planning.ton-domaine.fr` sur ton téléphone (en 4G) : la page de connexion doit s'afficher avec le cadenas.
Connecte-toi en admin avec le mot de passe mis dans `.env`.

## Étape 5 — Sauvegardes automatiques (5 min)

Test d'une sauvegarde manuelle, **Serveur** :

```bash
cd ~/planning-securite
bash deploy/sauvegarde.sh
```

Doit afficher `Sauvegarde OK : …/sauvegardes/plansecu-….sql.gz`.

Programme-la chaque nuit à 3h :

```bash
(crontab -l 2>/dev/null; echo "0 3 * * * bash $HOME/planning-securite/deploy/sauvegarde.sh >> $HOME/planning-securite/sauvegardes/journal.log 2>&1") | crontab -
crontab -l
```

La dernière commande doit afficher la ligne ajoutée. Les sauvegardes de plus de 30 jours sont supprimées automatiquement.

> ⚠ Une sauvegarde qui reste sur le même serveur ne protège pas contre la perte du serveur.
> Une fois par semaine (ou après une grosse saisie), récupère la dernière sur ton PC, **PC** :
>
> ```powershell
> scp "ubuntu@IP_DU_SERVEUR:~/planning-securite/sauvegardes/*.sql.gz" "$env:USERPROFILE\Documents\"
> ```

**Restaurer** une sauvegarde (remplace toutes les données, demande confirmation, et sauvegarde l'état actuel avant) :

```bash
cd ~/planning-securite
ls sauvegardes/
bash deploy/restauration.sh sauvegardes/plansecu-AAAA-MM-JJ_HHMM.sql.gz
```

## Étape 6 — Premiers réglages dans l'appli

1. Connecte-toi en admin → **Mon compte** : vérifie que tu peux changer le mot de passe.
2. **Lieux** : crée tes magasins, puis **Tous les QR codes → Imprimer**. Les QR codes contiennent l'adresse
   `https://planning.ton-domaine.fr` : ils fonctionneront depuis n'importe quel téléphone.
3. **Agents** : crée les fiches, avec identifiant + mot de passe pour ceux qui doivent pointer.
4. Informe les agents du pointage par QR code (et le CSE s'il y en a un), et ajoute le traitement à ton registre RGPD.

---

## Mettre à jour l'appli plus tard

Quand je t'envoie un nouveau zip :

**PC :**

```powershell
scp "$env:USERPROFILE\Downloads\planning-securite.zip" ubuntu@IP_DU_SERVEUR:~/
```

**Serveur :**

```bash
cd ~ && unzip -o planning-securite.zip && cd ~/planning-securite && chmod +x deploy/*.sh
bash deploy/mise-a-jour.sh
```

Le script sauvegarde la base, reconstruit, redémarre et affiche les logs (Ctrl+C pour quitter).
Ton fichier `.env` et tes sauvegardes ne sont pas dans le zip : ils sont conservés.

## Commandes utiles (serveur, dans `~/planning-securite`)

| Besoin | Commande |
|---|---|
| État des conteneurs | `docker compose ps` |
| Logs de l'appli | `docker compose logs --tail=100 app` |
| Redémarrer l'appli | `docker compose restart app` |
| Mémoire utilisée | `docker stats --no-stream` |
| Console SQL | `docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" plansecu'` |
| Arrêter PlanSécu (données conservées) | `docker compose down` |

> ⛔ Ne jamais taper `docker compose down -v` en production : le `-v` **efface la base de données**.
