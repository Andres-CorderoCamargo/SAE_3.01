-- =====================================================================
-- SAE 3.01 - Vote electronique - Sprint 1
-- Fichier 3 sur 4 : donnees d'essai du sprint 1.
INSERT INTO Utilisateurs
 (idUtilisateur, identifiantUtilisateur, mdpHashUtilisateur, roleUtilisateur, doitChangerMdp)
VALUES ('U1', 'gasquet', 'hash_a_remplacer', 'ORGANISATEUR', 0);

INSERT INTO Utilisateurs
 (idUtilisateur, identifiantUtilisateur, mdpHashUtilisateur, roleUtilisateur, doitChangerMdp)
VALUES ('U2', 'scrutateur1', 'hash_a_remplacer', 'SCRUTATEUR', 0);

INSERT INTO Utilisateurs
 (idUtilisateur, identifiantUtilisateur, mdpHashUtilisateur, roleUtilisateur)
VALUES ('U3', 'alice', 'hash_a_remplacer', 'ELECTEUR');

INSERT INTO Utilisateurs
 (idUtilisateur, identifiantUtilisateur, mdpHashUtilisateur, roleUtilisateur)
VALUES ('U4', 'bob', 'hash_a_remplacer', 'ELECTEUR');

INSERT INTO Utilisateurs
 (idUtilisateur, identifiantUtilisateur, mdpHashUtilisateur, roleUtilisateur)
VALUES ('U5', 'chloe', 'hash_a_remplacer', 'ELECTEUR');

INSERT INTO Electeurs VALUES ('U3', 'Martin', 'Alice');
INSERT INTO Electeurs VALUES ('U4', 'Durand', 'Bob');
INSERT INTO Electeurs VALUES ('U5', 'Petit', 'Chloe');

INSERT INTO Referendums
 (idReferendum, titreReferendum, questionReferendum,
  dateOuverture, dateFermeture, idOrganisateur)
VALUES ('R1', 'Teletravail',
        'Acceptez-vous le passage a 2 jours de teletravail par semaine ?',
        TO_DATE('10/10/2026 08:00', 'DD/MM/YYYY HH24:MI'),
        TO_DATE('11/10/2026 18:00', 'DD/MM/YYYY HH24:MI'),
        'U1');

INSERT INTO Referendums
 (idReferendum, titreReferendum, questionReferendum,
  dateOuverture, dateFermeture, idOrganisateur)
VALUES ('R2', 'Cantine',
        'Souhaitez-vous une option vegetarienne quotidienne ?',
        TO_DATE('01/12/2026 08:00', 'DD/MM/YYYY HH24:MI'),
        TO_DATE('05/12/2026 18:00', 'DD/MM/YYYY HH24:MI'),
        'U1');

INSERT INTO Inscriptions (idReferendum, idUtilisateur) VALUES ('R1', 'U3');
INSERT INTO Inscriptions (idReferendum, idUtilisateur) VALUES ('R1', 'U4');
INSERT INTO Inscriptions (idReferendum, idUtilisateur) VALUES ('R1', 'U5');

COMMIT;


SELECT idUtilisateur, identifiantUtilisateur, roleUtilisateur FROM Utilisateurs;
SELECT idReferendum, titreReferendum, statutReferendum FROM Referendums;
SELECT * FROM Inscriptions;
