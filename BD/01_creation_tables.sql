-- =====================================================================
-- SAE 3.01 - Vote electronique - Sprint 1
-- Creation des tables sous Oracle (iSQL*Plus)
-- Fichier 1 sur 4 : les tables et la vue, rien d'autre.
-- Les donnees d'essai sont dans 03_donnees_essai.sql.
-- =====================================================================
-- Au tout premier lancement, les 6 DROP affichent une erreur
-- "table ou vue inexistante" : c'est normal, on continue.

DROP TABLE Resultats CASCADE CONSTRAINT;
DROP TABLE UrnesChiffrees CASCADE CONSTRAINT;
DROP TABLE Inscriptions CASCADE CONSTRAINT;
DROP TABLE Referendums CASCADE CONSTRAINT;
DROP TABLE Electeurs CASCADE CONSTRAINT;
DROP TABLE Utilisateurs CASCADE CONSTRAINT;


-- ---------------------------------------------------------------------
-- Utilisateurs : un compte, un role, un mot de passe HACHE.
-- ---------------------------------------------------------------------
CREATE TABLE Utilisateurs
(idUtilisateur VARCHAR2(5),
 identifiantUtilisateur VARCHAR2(25) NOT NULL,
 mdpHashUtilisateur VARCHAR2(100) NOT NULL,
 roleUtilisateur VARCHAR2(12) NOT NULL,
 doitChangerMdp NUMBER(1) DEFAULT 1 NOT NULL,
 tentativesEchouees NUMBER DEFAULT 0 NOT NULL,
 verrouilleJusqua DATE,
 actifUtilisateur NUMBER(1) DEFAULT 1 NOT NULL,
CONSTRAINT pk_Utilisateurs PRIMARY KEY (idUtilisateur),
CONSTRAINT un_Utilisateurs_identifiant UNIQUE (identifiantUtilisateur),
CONSTRAINT ck_Utilisateurs_role
    CHECK (roleUtilisateur IN ('ELECTEUR', 'ORGANISATEUR', 'SCRUTATEUR')),
CONSTRAINT ck_Utilisateurs_changerMdp CHECK (doitChangerMdp IN (0, 1)),
CONSTRAINT ck_Utilisateurs_actif CHECK (actifUtilisateur IN (0, 1)),
CONSTRAINT ck_Utilisateurs_tentatives CHECK (tentativesEchouees >= 0));


-- ---------------------------------------------------------------------
-- Electeurs : identite de l'electeur. Heritage par cle primaire, comme
-- en TD2 : la cle primaire est aussi cle etrangere vers Utilisateurs.
-- ---------------------------------------------------------------------
CREATE TABLE Electeurs
(idUtilisateur VARCHAR2(5),
 nomElecteur VARCHAR2(25) NOT NULL,
 prenomElecteur VARCHAR2(25) NOT NULL,
CONSTRAINT pk_Electeurs PRIMARY KEY (idUtilisateur),
CONSTRAINT fk_Electeurs_idUtilisateur
    FOREIGN KEY (idUtilisateur) REFERENCES Utilisateurs(idUtilisateur));


-- ---------------------------------------------------------------------
-- Referendums : la question, la periode de vote et l'etat du scrutin.
-- Les 4 etats reprennent le modele de donnees valide par l'equipe.
-- ---------------------------------------------------------------------
CREATE TABLE Referendums
(idReferendum VARCHAR2(5),
 titreReferendum VARCHAR2(50) NOT NULL,
 questionReferendum VARCHAR2(200) NOT NULL,
 dateOuverture DATE NOT NULL,
 dateFermeture DATE NOT NULL,
 statutReferendum VARCHAR2(15) DEFAULT 'EN_PREPARATION' NOT NULL,
 idOrganisateur VARCHAR2(5) NOT NULL,
CONSTRAINT pk_Referendums PRIMARY KEY (idReferendum),
CONSTRAINT fk_Referendums_idOrganisateur
    FOREIGN KEY (idOrganisateur) REFERENCES Utilisateurs(idUtilisateur),
CONSTRAINT ck_Referendums_statut
    CHECK (statutReferendum IN
           ('EN_PREPARATION', 'OUVERT', 'CLOTURE', 'DEPOUILLE')),
CONSTRAINT ck_Referendums_periode CHECK (dateOuverture < dateFermeture));


-- ---------------------------------------------------------------------
-- Inscriptions : qui a le droit de voter a quel referendum.
-- dateVote reste NULL tant que l'electeur n'a pas vote. Cette table ne
-- contient NI le choix de l'electeur NI son bulletin chiffre : elle sert
-- uniquement a garantir "un electeur, une voix".
-- ---------------------------------------------------------------------
CREATE TABLE Inscriptions
(idReferendum VARCHAR2(5),
 idUtilisateur VARCHAR2(5),
 dateVote DATE,
CONSTRAINT pk_Inscriptions PRIMARY KEY (idReferendum, idUtilisateur),
CONSTRAINT fk_Inscriptions_idReferendum
    FOREIGN KEY (idReferendum) REFERENCES Referendums(idReferendum),
CONSTRAINT fk_Inscriptions_idUtilisateur
    FOREIGN KEY (idUtilisateur) REFERENCES Electeurs(idUtilisateur));


-- ---------------------------------------------------------------------
-- UrnesChiffrees : la cle PUBLIQUE (p, g, h) du scrutateur et l'agregat
-- chiffre (u, v) des bulletins deja recus.
-- La cle PRIVEE x ne figure nulle part : elle reste sur le poste du
-- scrutateur, comme l'impose le sujet.
-- Les grands entiers ElGamal sont stockes en texte decimal : c'est le
-- format accepte par le constructeur BigInteger(String) en Java.
-- L'agregat demarre a (1, 1) : c'est Encrypt(0) avec r = 0, donc
-- l'element neutre de la methode Agrege.
-- ---------------------------------------------------------------------
CREATE TABLE UrnesChiffrees
(idReferendum VARCHAR2(5),
 idScrutateur VARCHAR2(5) NOT NULL,
 clePubliqueP VARCHAR2(4000) NOT NULL,
 clePubliqueG VARCHAR2(4000) NOT NULL,
 clePubliqueH VARCHAR2(4000) NOT NULL,
 agregatU VARCHAR2(4000) DEFAULT '1' NOT NULL,
 agregatV VARCHAR2(4000) DEFAULT '1' NOT NULL,
 nbVotesRecus NUMBER DEFAULT 0 NOT NULL,
CONSTRAINT pk_UrnesChiffrees PRIMARY KEY (idReferendum),
CONSTRAINT fk_UrnesChiffrees_idReferendum
    FOREIGN KEY (idReferendum) REFERENCES Referendums(idReferendum),
CONSTRAINT fk_UrnesChiffrees_idScrutateur
    FOREIGN KEY (idScrutateur) REFERENCES Utilisateurs(idUtilisateur),
CONSTRAINT ck_UrnesChiffrees_nbVotes CHECK (nbVotesRecus >= 0));


-- ---------------------------------------------------------------------
-- Resultats : le decompte GLOBAL publie apres le depouillement.
-- Aucun lien avec l'identite d'un electeur.
-- ---------------------------------------------------------------------
CREATE TABLE Resultats
(idReferendum VARCHAR2(5),
 voixPour NUMBER NOT NULL,
 nbVotesDepouilles NUMBER NOT NULL,
 datePublication DATE DEFAULT SYSDATE NOT NULL,
CONSTRAINT pk_Resultats PRIMARY KEY (idReferendum),
CONSTRAINT fk_Resultats_idReferendum
    FOREIGN KEY (idReferendum) REFERENCES Referendums(idReferendum),
CONSTRAINT ck_Resultats_voixPour CHECK (voixPour >= 0),
CONSTRAINT ck_Resultats_nbVotes CHECK (nbVotesDepouilles >= 0),
CONSTRAINT ck_Resultats_coherence CHECK (voixPour <= nbVotesDepouilles));


-- ---------------------------------------------------------------------
-- Vue publique : seuls les referendums DEPOUILLES sont consultables.
-- Les votes individuels restent secrets, le total est public.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW ResultatsPublics AS
SELECT r.idReferendum,
       r.titreReferendum,
       r.questionReferendum,
       s.voixPour,
       s.nbVotesDepouilles - s.voixPour AS voixContre,
       s.nbVotesDepouilles,
       s.datePublication
FROM Referendums r
JOIN Resultats s ON s.idReferendum = r.idReferendum
WHERE r.statutReferendum = 'DEPOUILLE';
