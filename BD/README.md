# La base de données — mode d'emploi

Base du sprint 1. Tout se colle dans iSQL*Plus :
`orainfo.iutmontp.univ-montp2.fr:5560/isqlplus`

## Comment la lancer

Quatre fichiers, dans cet ordre.

| | Fichier | Ce qu'il fait |
|---|---|---|
| 1 | `01_creation_tables.sql` | crée les 4 tables |
| 2 | `02_procedures.sql` | crée les 8 procédures |
| 3 | `03_donnees_essai.sql` | ajoute des comptes et 2 référendums pour essayer |
| 4 | `04_jeu_essai.sql` | vérifie que tout marche |

A ne pas oublier :

```sql
SET SERVEROUTPUT ON;
```

## Ce qu'il y a dans la base

| Table | Ce qu'elle contient |
|---|---|
| `Utilisateurs` | les comptes : identifiant, mot de passe haché, rôle |
| `Electeurs` | le nom et le prénom d'un électeur |
| `Referendums` | la question, les dates, l'état du vote |
| `Inscriptions` | qui a le droit de voter, et s'il a déjà voté |

`Inscriptions` est là dès maintenant parce qu'une tâche du sprint en a
besoin : refuser la suppression d'un électeur qui a déjà voté.

## Les 8 procédures

Une par tâche du sprint.

| Procédure | Story |
|---|---|
| `echecConnexion` · `succesConnexion` | S'authentifier |
| `ajouterElecteur` | Ajouter un électeur |
| `modifierElecteur` | Modifier un électeur |
| `supprimerElecteur` | Supprimer un électeur |
| `creerReferendum` | Créer un référendum |
| `modifierReferendum` | Modifier un référendum |
| `supprimerReferendum` | Supprimer un référendum |





## Ce que la base fait respecter

- Un électeur ne peut être rattaché qu'à un compte de rôle `ELECTEUR`,
  un référendum créé que par un `ORGANISATEUR`.
- Un référendum lancé ne se modifie plus et ne se supprime plus (A2).
- Un électeur qui a déjà voté ne peut pas être supprimé : on désactive
  son compte pour garder l'historique (A3).
- Deux comptes ne peuvent pas avoir le même identifiant (A1).
- Après 3 mots de passe erronés, le compte est bloqué 15 minutes (A2).

## Ce qui reste à faire côté Java

- **Hacher les mots de passe.** La base attend une empreinte. Tant que
  ce n'est pas branché, on met `hash_a_remplacer`.
- **Appeler les procédures en JDBC**, avec `CallableStatement` et
  `registerOutParameter` pour récupérer le message.

## Attention avec le fichier 3

**C'est uniquement pour les
essais.** Pour une vraie installation, on lance les fichiers 1 et 2,
pas le 3.
