-- SAE 3.01 - Vote electronique - Sprint 1
-- Creation des tables sous Oracle (iSQL*Plus)


DROP TABLE Inscriptions CASCADE CONSTRAINT;
DROP TABLE Referendums CASCADE CONSTRAINT;
DROP TABLE Electeurs CASCADE CONSTRAINT;
DROP TABLE Utilisateurs CASCADE CONSTRAINT;


-- ---------------------------------------------------------------------
-- Utilisateurs : un compte, un role, un mot de passe HACHE.
-- ---------------------------------------------------------------------
CREATE TABLE Utilisateurs
(idUtilisateur VARCHAR(5),
 identifiantUtilisateur VARCHAR(25) NOT NULL,
 mdpHashUtilisateur VARCHAR(100) NOT NULL,
 roleUtilisateur VARCHAR(12) NOT NULL,
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
-- Electeurs : identite de l'electeur. Heritage par cle primaire.
-- ---------------------------------------------------------------------
CREATE TABLE Electeurs
(idUtilisateur VARCHAR(5),
 nomElecteur VARCHAR(25) NOT NULL,
 prenomElecteur VARCHAR(25) NOT NULL,
CONSTRAINT pk_Electeurs PRIMARY KEY (idUtilisateur),
CONSTRAINT fk_Electeurs_idUtilisateur
    FOREIGN KEY (idUtilisateur) REFERENCES Utilisateurs(idUtilisateur));


-- ---------------------------------------------------------------------
-- Referendums : la question, la periode de vote et l'etat du scrutin.
-- ---------------------------------------------------------------------
CREATE TABLE Referendums
(idReferendum VARCHAR(5),
 titreReferendum VARCHAR(50) NOT NULL,
 questionReferendum VARCHAR(200) NOT NULL,
 dateOuverture DATE NOT NULL,
 dateFermeture DATE NOT NULL,
 statutReferendum VARCHAR(15) DEFAULT 'EN_PREPARATION' NOT NULL,
 idOrganisateur VARCHAR(5) NOT NULL,
CONSTRAINT pk_Referendums PRIMARY KEY (idReferendum),
CONSTRAINT fk_Referendums_idOrganisateur
    FOREIGN KEY (idOrganisateur) REFERENCES Utilisateurs(idUtilisateur),
CONSTRAINT ck_Referendums_statut
    CHECK (statutReferendum IN
           ('EN_PREPARATION', 'OUVERT', 'CLOTURE', 'DEPOUILLE')),
CONSTRAINT ck_Referendums_periode CHECK (dateOuverture < dateFermeture));


-- ---------------------------------------------------------------------
-- Inscriptions : qui a le droit de voter a quel referendum.
-- dateVote reste NULL tant que l'electeur n'a pas vote. Cette table elle sert
-- uniquement a garantir "un electeur, une voix".
-- ---------------------------------------------------------------------
CREATE TABLE Inscriptions
(idReferendum VARCHAR(5),
 idUtilisateur VARCHAR(5),
 dateVote DATE,
CONSTRAINT pk_Inscriptions PRIMARY KEY (idReferendum, idUtilisateur),
CONSTRAINT fk_Inscriptions_idReferendum
    FOREIGN KEY (idReferendum) REFERENCES Referendums(idReferendum),
CONSTRAINT fk_Inscriptions_idUtilisateur
    FOREIGN KEY (idUtilisateur) REFERENCES Electeurs(idUtilisateur));
