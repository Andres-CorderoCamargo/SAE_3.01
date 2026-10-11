# La base de données — mode d'emploi

Base du vote électronique, écrite pour Oracle. Tout se colle dans
iSQL*Plus : `orainfo.iutmontp.univ-montp2.fr:5560/isqlplus`

## Comment la lancer

Quatre fichiers, dans cet ordre. On colle le fichier, on clique
**Exécuter**, on passe au suivant.

| | Fichier | Ce qu'il fait |
|---|---|---|
| 1 | `01_creation_tables.sql` | crée les 6 tables |
| 2 | `02_procedures.sql` | crée les 18 fonctions et procédures |
| 3 | `03_donnees_essai.sql` | ajoute des comptes et 2 référendums pour essayer |
| 4 | `04_jeu_essai.sql` | vérifie que tout marche |

Avant de commencer, tape une fois :

```sql
SET SERVEROUTPUT ON;
```

Sans ça, rien ne s'affiche.

## Deux choses normales qui ressemblent à des erreurs

**Au tout premier lancement**, le fichier 1 affiche six fois
`ORA-00942 : Table ou vue inexistante`. C'est normal : il essaie
d'effacer des tables qui n'existent pas encore. Les fois suivantes,
ce message disparaît.


## Si tu veux recommencer

Relance les quatre fichiers dans l'ordre. Le fichier 1 efface tout et
repart de zéro.

Le fichier 4 ne se relance pas tout seul : il déroule un référendum
du début jusqu'au dépouillement. Une fois le référendum dépouillé, on
ne peut plus voter dedans — c'est justement ce que la base doit
interdire.

## Attention avec le fichier 3

Il crée Alice, Bob, Chloé, un organisateur et un scrutateur, avec
`hash_a_remplacer` comme mot de passe. **C'est uniquement pour les
essais.** Le jour où on installe la base pour de vrai, on lance les
fichiers 1 et 2, jamais le 3.

## Ce qu'il y a dans la base

| Table | Ce qu'elle contient |
|---|---|
| `Utilisateurs` | les comptes : identifiant, mot de passe haché, rôle |
| `Electeurs` | le nom et le prénom d'un électeur |
| `Referendums` | la question, les dates, l'état du vote |
| `Inscriptions` | qui a le droit de voter, et s'il a déjà voté |
| `UrnesChiffrees` | la clé publique et le total chiffré des bulletins |
| `Resultats` | le nombre de oui, publié à la fin |

Plus `ResultatsPublics` : une vue qui ne montre que les référendums
dépouillés.

**Ce qui n'y est pas, et n'y sera jamais :** le choix d'un électeur,
son bulletin, et la clé privée du scrutateur. On sait **qui** a voté,
jamais **ce qu'il** a voté.

## Les procédures que Java va appeler

| Procédure | À quoi elle sert |
|---|---|
| `echecConnexion` · `succesConnexion` | se connecter, bloquer après 3 erreurs |
| `ajouterElecteur` · `modifierElecteur` · `supprimerElecteur` | gérer les électeurs |
| `inscrireElecteur` | inscrire quelqu'un à un référendum |
| `creerReferendum` · `modifierReferendum` · `supprimerReferendum` | gérer les référendums |
| `lancerReferendum` · `cloturerReferendum` | ouvrir et fermer le vote |
| `enregistrerVote` | **voter** — la plus importante |
| `publierResultat` | publier le résultat après dépouillement |
| `affichageResultat` · `affichageTousReferendums` | afficher |
| `nbElecteursInscrits` · `nbVotants` · `aDejaVote` | compter |

Chaque procédure renvoie un message :

- `OK` → c'est fait
- autre chose → c'est refusé, et le texte dit pourquoi

C'est ce texte que le programme Java affiche à l'utilisateur.

Pour appeler une procédure à la main dans iSQL*Plus :

```sql
DECLARE
    v_message VARCHAR2(200);
BEGIN
    lancerReferendum('R1', v_message);
    DBMS_OUTPUT.PUT_LINE(v_message);
END;
/
```

## Qui fait quoi : Java ou Oracle

Oracle ne sait pas multiplier des nombres de 512 chiffres. Donc :

- **Java** chiffre les bulletins, les additionne, déchiffre le total,
  et hache les mots de passe.
- **Oracle** range les données et fait respecter les règles du vote.

Pour un vote : Java chiffre le bulletin, calcule le nouveau total
chiffré `(u, v)`, et appelle `enregistrerVote` avec ces deux valeurs.
La procédure vérifie que le vote est ouvert, que la date est bonne,
que la personne est inscrite et qu'elle n'a pas déjà voté. Puis elle
enregistre le vote **et** le total en même temps : soit les deux, soit
aucun des deux.

Les grands nombres sont rangés en texte. C'est ce que lit
`new BigInteger("...")` en Java.

## Ce qu'il reste à faire côté Java

- **Hacher les mots de passe.** La base attend une empreinte, mais
  rien ne la fabrique encore. Tant que ce n'est pas fait, on met
  `hash_a_remplacer`.
- **Vérifier qu'un bulletin chiffré vaut bien 0 ou 1.** Ni SQL ni
  PL/SQL ne peuvent le prouver.
- **Choisir la durée du blocage.** J'ai mis 15 minutes dans
  `echecConnexion`, à changer si vous voulez autre chose.
