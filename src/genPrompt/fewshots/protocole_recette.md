# Exemples calibrés — Protocole & Recette

## EXEMPLE

### DEMANDE
Donne-moi la procedure pour mettre en place une sauvegarde automatique d'une base Postgres.

### REPONSE
**Prérequis**
- Accès `postgres` ou rôle disposant de `pg_read_all_data`
- `pg_dump` installé, même version majeure que le serveur
- Un emplacement de destination avec une rétention définie

**Procédure**

1. **Créer le rôle de sauvegarde** — un rôle dédié en lecture seule, jamais le superutilisateur.
   ```sql
   CREATE ROLE sauvegarde LOGIN PASSWORD '...';
   GRANT pg_read_all_data TO sauvegarde;
   ```
2. **Écrire le script de dump** — le format `custom` permet la restauration sélective.
   ```bash
   pg_dump -Fc -Z9 -f "/backups/app_$(date +%F).dump" app
   ```
3. **Planifier l'exécution** — hors des heures de charge.
   ```cron
   0 3 * * * /usr/local/bin/sauvegarde-app.sh
   ```
4. **Appliquer la rétention** — sans purge, le disque se remplit silencieusement.
   ```bash
   find /backups -name 'app_*.dump' -mtime +30 -delete
   ```
5. **Vérifier par une restauration réelle**, dans une base jetable.
   ```bash
   pg_restore -d app_test /backups/app_2026-01-15.dump
   ```

**Points de contrôle**
- L'étape 5 est la seule qui prouve quoi que ce soit : une sauvegarde jamais restaurée n'est pas une sauvegarde.
- Surveiller le code de sortie de `pg_dump` : un dump tronqué se termine sans erreur visible dans le fichier.
