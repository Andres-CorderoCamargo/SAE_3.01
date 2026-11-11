-- Affiche l'etat de la base (a lancer apres chaque scenario pour verifier).
SET LINESIZE 200
SET PAGESIZE 100
COLUMN identifiantUtilisateur FORMAT A20
COLUMN roleUtilisateur FORMAT A14
COLUMN debut_hash FORMAT A10
COLUMN titreReferendum FORMAT A20

PROMPT === Utilisateurs (longueur et debut du hash) ===
SELECT idUtilisateur, identifiantUtilisateur, roleUtilisateur,
       LENGTH(mdpHashUtilisateur) AS long_hash,
       SUBSTR(mdpHashUtilisateur, 1, 7) AS debut_hash,
       doitChangerMdp AS chg_mdp, tentativesEchouees AS tent, actifUtilisateur AS actif
FROM Utilisateurs ORDER BY idUtilisateur;

PROMPT === Electeurs ===
SELECT * FROM Electeurs ORDER BY idUtilisateur;

PROMPT === Referendums ===
SELECT idReferendum, titreReferendum, statutReferendum FROM Referendums ORDER BY idReferendum;

PROMPT === Inscriptions ===
SELECT * FROM Inscriptions ORDER BY idReferendum, idUtilisateur;
