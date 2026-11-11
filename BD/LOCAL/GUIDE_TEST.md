# Tester en local avec une base Oracle jetable

Pas besoin de la base de l'IUT : on lance un Oracle dans Docker, avec **les mêmes
scripts** que la vraie base (`01_creation_tables.sql`, `02_procedures.sql`,
`TEST/03_donnees_essai.sql`). On peut tout casser, puis tout remettre à zéro.

Prérequis : Docker Desktop (ou Docker + le plugin compose), Java, Maven.
Les commandes sont écrites pour bash (Linux, macOS, Git Bash, WSL).
Les équivalents PowerShell sont donnés à l'étape 2.

---

## 1. Démarrer la base

```bash
cd BD/LOCAL
docker compose up -d
docker compose ps        # attendre que l'état passe à "healthy" (1 à 3 minutes la 1re fois)
```

## 2. Variables d'environnement (à refaire dans CHAQUE terminal)

```bash
source BD/LOCAL/env.sh          # depuis la racine du projet
```

PowerShell :

```powershell
$env:ORACLE_URL      = "jdbc:oracle:thin:@//localhost:1521/FREEPDB1"
$env:ORACLE_USER     = "sae"
$env:ORACLE_PASSWORD = "sae_local"
$env:SAE_POIVRE      = "poivre-de-test-local"
```

## 3. Créer les tables, les procédures et les données d'essai

```bash
docker exec -i sae-oracle sqlplus -s sae/sae_local@//localhost:1521/FREEPDB1 @/bd/LOCAL/reset.sql
```

Les erreurs `ORA-00942` au tout premier lancement sont normales (les `DROP TABLE`
visent des tables qui n'existent pas encore). Le script se termine par l'état de la
base : 5 utilisateurs (`gasquet`, `scrutateur1`, `alice`, `bob`, `chloe`),
2 référendums, 3 inscriptions.

**Cette commande remet tout à zéro : relance-la avant chaque série de tests.**

## 4. Compiler le projet (une seule fois, puis après chaque modification)

```bash
mvn -q clean install -DskipTests
```

## 5. Mettre de vrais mots de passe aux comptes d'essai

Le fichier 03 met `hash_a_remplacer` comme mot de passe : personne ne peut se connecter.
Cet outil le remplace par le vrai hachage (poivre + bcrypt) du mot de passe `Test1234!`
et vérifie la connexion.

```bash
mvn -q -pl app-systeme exec:java \
  -Dexec.mainClass=systeme.persistance.tests.OutilComptesEssai \
  -Dexec.classpathScope=test
```

Attendu :

```
5 compte(s) mis a jour : [gasquet, scrutateur1, alice, bob, chloe]
Connexion de 'gasquet' avec un mauvais mot de passe -> refuse (normal) (Identifiant ou mot de passe incorrect)
Connexion de 'gasquet' avec le bon mot de passe -> OK (OK)
```

Vérifier en base :

```bash
docker exec -i sae-oracle sqlplus -s sae/sae_local@//localhost:1521/FREEPDB1 @/bd/LOCAL/etat.sql
```

Les 5 comptes doivent avoir `long_hash = 60` et `debut_hash = $2a$12$`.

---

## 6. Les tests, dans l'ordre

### Test A : tests unitaires (pas besoin de la base)

```bash
mvn test
```

Attendu : `BUILD SUCCESS` (`MotDePasseTest` : 33 tests, `DechiffrementTest` : 17 tests).

### Test B : procédures SQL (le jeu d'essai du sprint 1)

À lancer juste après l'étape 3 (base remise à zéro), pas après le test C :

```bash
docker exec -i sae-oracle sqlplus -s sae/sae_local@//localhost:1521/FREEPDB1 @/bd/TEST/04_jeu_essai.sql
```

Il affiche une ligne par cas (1a à 6d) avec le message de la procédure. Compare avec
les attentes écrites dans le fichier. Ça teste la base seule, sans Java.

### Test C : Java + base (hachage, connexion, blocage)

```bash
mvn -q -pl app-systeme exec:java \
  -Dexec.mainClass=systeme.persistance.tests.TestAuthentification \
  -Dexec.classpathScope=test
```

Attendu : `Résultat : N OK, 0 KO`. Il crée ses propres comptes (`T901`, `T902`) et
les efface à la fin, on peut le relancer.

### Test D : serveur + client, scénarios à la main

Terminal 1 (serveur) :

```bash
source BD/LOCAL/env.sh
mvn -q -pl app-systeme exec:java -Dexec.mainClass=systeme.systeme
```

Terminal 2 (client électeur) :

```bash
mvn -q -pl app-electeur exec:java -Dexec.mainClass=electeur.electeur
```

Dans le menu du client, fais les scénarios ci-dessous, **dans l'ordre**. Après chacun,
tu peux relancer `etat.sql` (commande de l'étape 5) pour voir la base.

**Connexion (menu 2)**

| # | Saisie | Résultat attendu côté client | À vérifier en base |
|---|---|---|---|
| D1 | `chloe` / `Test1234!` | « Connecté en tant que ELECTEUR — mot de passe à changer » | `chg_mdp = 1` |
| D2 | `inconnu` / `nimportequoi` | « Identifiant ou mot de passe incorrect » | rien |
| D3 | `bob` / `MauvaisMdp`, trois fois | 2× « Identifiant ou mot de passe incorrect », puis « Compte bloque 15 minutes apres 3 echecs » | `tent = 3` pour bob |
| D4 | `bob` / `Test1234!` (le bon, juste après) | « Compte bloque jusqu'a HH:MI » | toujours bloqué |
| D5 | `gasquet` / `Test1234!` | « Connecté en tant que ORGANISATEUR » | `tent = 0` |

**Gestion des électeurs (menus 1, 4, 5) : réservée à l'organisateur**

Depuis le commit `f4ac08a` du 11/11, le serveur exige le jeton d'une session
organisateur pour inscrire, modifier ou supprimer. Il faut donc avoir fait D5 juste avant.

Le client envoie ce jeton depuis le commit `21f0f02`. Si D6 à D13 répondent
« Action réservée à l'organisateur. Connectez-vous d'abord. », c'est que tu n'as pas fait
D5 dans la même session du client.

| # | Menu | Données à saisir | Résultat attendu côté client | À vérifier en base |
|---|---|---|---|---|
| D6 | 1 Inscription | id `U10`, login `dora`, mdp `Test1234!`, nom `Roux`, prénom `Dora` | Inscription réussie | `U10` existe, `long_hash=60`, `debut_hash=$2a$12$`, mdp **pas** en clair |
| D7 | 1 Inscription | id `U11`, login `dora` (déjà pris) | « Cet identifiant est deja utilise » | pas de `U11` |
| D8 | 1 Inscription | id `ABCDEF` (6 caractères) | « Identifiant electeur invalide : 5 caracteres maximum » | rien ajouté |
| D9 | 1 Inscription | mot de passe vide | « tous les champs sont obligatoires » | rien ajouté |
| D10 | 5 Modification | id `U10`, login `dora2`, nom `Roux`, prénom `Dora` | `OK` | login de `U10` = `dora2` |
| D11 | 5 Modification | id `U99` (inconnu) | « Cet electeur n'existe pas » | rien |
| D12 | 4 Suppression | id `U10`, confirmer | `OK` | `U10` a disparu |
| D13 | 4 Suppression | id `U3` (Alice), **après** la commande SQL ci-dessous | « Electeur ayant deja vote : compte desactive, suppression impossible » | `U3` toujours là, `actif = 0` |

Pour D13, simuler un vote d'Alice (dans un 3e terminal) :

```bash
docker exec -i sae-oracle sqlplus -s sae/sae_local@//localhost:1521/FREEPDB1 <<'EOF'
UPDATE Inscriptions SET dateVote = SYSDATE WHERE idReferendum = 'R1' AND idUtilisateur = 'U3';
COMMIT;
EXIT
EOF
```

Après D13, `alice` / `Test1234!` au menu 2 doit répondre le même message que D2
(compte désactivé, mais on ne dit pas pourquoi).

### Test E : le poivre manquant (le cas ajouté dans `systeme.java`)

1. Arrête le serveur (Ctrl+C).
2. Retire le poivre : `unset SAE_POIVRE` (PowerShell : `Remove-Item Env:SAE_POIVRE`).
3. Relance le serveur, puis dans le client : menu 2, `gasquet` / `Test1234!`.
4. Attendu côté client : « Erreur interne du serveur, réessayez plus tard. »
   Attendu côté serveur : `Vérification impossible : Poivre non defini ...`,
   **et le serveur continue de tourner** (refais une connexion pour le vérifier).
5. Remets le poivre (`source BD/LOCAL/env.sh`) et relance le serveur.

### Test F : changement de poivre

**F1. Automatique (recommandé)** : joue tout le scénario sur la base avec deux poivres
d'essai, sans toucher à `SAE_POIVRE`.

```bash
mvn -q -pl app-systeme exec:java \
  -Dexec.mainClass=systeme.persistance.tests.EssaiRotationPoivre \
  -Dexec.classpathScope=test
```

Attendu : `Résultat : 10 OK, 0 KO`. Les étapes affichées :
1. nouveau poivre seul : connexion refusée (le compte « perd » son mot de passe) ;
2. mauvais mot de passe pendant la transition : refusé ;
3. bon mot de passe pendant la transition : accepté **et** empreinte refaite ;
4. ancien poivre retiré : le compte marche toujours.

**F2. À la main, avec le serveur et le client** (comme en vrai, cf. `README.md`,
section « Changer le poivre ») :

1. Repars d'une base propre : étape 3, puis étape 5 avec le poivre de `env.sh`.
2. Arrête le serveur. Dans son terminal, passe en transition :
   ```bash
   export SAE_POIVRE_ANCIEN="poivre-de-test-local"   # l'ancien (celui de env.sh)
   export SAE_POIVRE="nouveau-poivre-de-test"         # le nouveau
   ```
   Relance le serveur.
3. Note l'empreinte actuelle de `gasquet` et `scrutateur1` :
   ```bash
   docker exec -i sae-oracle sqlplus -s sae/sae_local@//localhost:1521/FREEPDB1 <<'EOF'
   SELECT identifiantUtilisateur, mdpHashUtilisateur FROM Utilisateurs
   WHERE identifiantUtilisateur IN ('gasquet', 'scrutateur1');
   EXIT
   EOF
   ```
4. Dans le client : menu 2, `gasquet` / `Test1234!`. Attendu : connecté, et dans les
   logs du serveur `Empreinte de U1 refaite avec le nouveau poivre`.
5. Relance la requête de l'étape 3 : l'empreinte de `gasquet` a changé, celle de
   `scrutateur1` (qui ne s'est pas connecté) non.
6. Fin de transition : arrête le serveur, `unset SAE_POIVRE_ANCIEN`, relance-le.
7. Menu 2, `gasquet` / `Test1234!` : connecté (il a été migré).
   Menu 2, `scrutateur1` / `Test1234!` : refusé (pas migré à temps, il faudra lui
   redonner un mot de passe). C'est le comportement attendu.
8. Remets `source BD/LOCAL/env.sh` pour les autres tests.

---

## Tout arrêter / tout supprimer

```bash
docker compose stop            # garde les données
docker compose down -v         # supprime la base de test
```

## Si ça ne marche pas

- **`ORA-12541` / connexion refusée** : la base n'est pas encore « healthy »
  (`docker compose ps`), ou le port 1521 est déjà utilisé.
- **`Variables ORACLE_URL ... non definies`** : tu as oublié `source BD/LOCAL/env.sh`
  dans ce terminal.
- **`SAE_POIVRE` non définie** : idem.
- **`ORA-01031` ou « insufficient privileges » à l'étape 3** : copie-moi le message,
  je corrigerai le compose.
- **Rien n'est vraiment poussé ni modifié sur la base de l'IUT** : tout ce guide ne
  touche que le conteneur local.
