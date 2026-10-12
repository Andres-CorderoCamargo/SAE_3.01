-- =====================================================================
-- SAE 3.01 - Vote electronique - Sprint 1
-- Jeu d'essai : on deroule un referendum complet et on verifie que
-- chaque regle du dossier d'analyse est bien appliquee.
-- Fichier 4 sur 4 : les essais.
-- A executer APRES les fichiers 01, 02 et 03.
-- =====================================================================
-- Les procedures ont un parametre OUT : on les appelle depuis un bloc
-- PL/SQL anonyme, pas avec CALL.

SET SERVEROUTPUT ON;


-- ---------------------------------------------------------------------
-- ESSAI 1 : on ne peut pas voter avant le lancement
-- ---------------------------------------------------------------------
DECLARE
    v_message VARCHAR2(200);
BEGIN
    enregistrerVote('R1', 'U3', '5', '17', v_message);
    DBMS_OUTPUT.PUT_LINE('1. Vote avant lancement  -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- ESSAI 2 : lancement refuse si la cle du scrutateur manque (R2)
--           puis lancement accepte pour R1
-- ---------------------------------------------------------------------
DECLARE
    v_message VARCHAR2(200);
BEGIN
    lancerReferendum('R2', v_message);
    DBMS_OUTPUT.PUT_LINE('2a. Lancement R2 sans cle -> ' || v_message);

    lancerReferendum('R1', v_message);
    DBMS_OUTPUT.PUT_LINE('2b. Lancement R1          -> ' || v_message);
END;
/

-- Le jeu d'essai place R1 en octobre 2026. Pour que les essais de vote
-- fonctionnent aujourd'hui, on elargit la periode a l'annee en cours.
UPDATE Referendums
SET dateOuverture = SYSDATE - 1,
    dateFermeture = SYSDATE + 30
WHERE idReferendum = 'R1';
COMMIT;


-- ---------------------------------------------------------------------
-- ESSAI 3 : Alice vote, puis essaie de voter une seconde fois
-- ---------------------------------------------------------------------
DECLARE
    v_message VARCHAR2(200);
BEGIN
    -- (u, v) calcules en Java par la methode Agrege. Ici des valeurs
    -- d'exemple avec la petite cle p=23, g=2, h=8.
    enregistrerVote('R1', 'U3', '4', '16', v_message);
    DBMS_OUTPUT.PUT_LINE('3a. Alice vote            -> ' || v_message);

    enregistrerVote('R1', 'U3', '8', '9', v_message);
    DBMS_OUTPUT.PUT_LINE('3b. Alice revote          -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- ESSAI 4 : un electeur non inscrit ne peut pas voter
-- ---------------------------------------------------------------------
DECLARE
    v_message VARCHAR2(200);
BEGIN
    -- On cree une electrice non inscrite a R1
    INSERT INTO Utilisateurs
      (idUtilisateur, identifiantUtilisateur, mdpHashUtilisateur, roleUtilisateur)
    VALUES ('U6', 'dora', 'hash_a_remplacer', 'ELECTEUR');
    INSERT INTO Electeurs VALUES ('U6', 'Lopez', 'Dora');
    COMMIT;

    enregistrerVote('R1', 'U6', '9', '3', v_message);
    DBMS_OUTPUT.PUT_LINE('4. Vote d''un non-inscrit -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- ESSAI 5 : Bob vote, Chloe ne vote pas
-- ---------------------------------------------------------------------
DECLARE
    v_message VARCHAR2(200);
BEGIN
    enregistrerVote('R1', 'U4', '16', '12', v_message);
    DBMS_OUTPUT.PUT_LINE('5. Bob vote               -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- ESSAI 6 : suppression d'electeur (scenario A3)
--   - Dora n'a jamais vote  -> supprimee
--   - Alice a vote          -> refus, compte desactive
-- ---------------------------------------------------------------------
DECLARE
    v_message VARCHAR2(200);
BEGIN
    supprimerElecteur('U6', v_message);
    DBMS_OUTPUT.PUT_LINE('6a. Supprimer Dora        -> ' || v_message);

    supprimerElecteur('U3', v_message);
    DBMS_OUTPUT.PUT_LINE('6b. Supprimer Alice       -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- ESSAI 7 : blocage apres 3 mauvais mots de passe (scenario A2)
-- ---------------------------------------------------------------------
DECLARE
    v_message VARCHAR2(200);
BEGIN
    echecConnexion('bob', v_message);
    DBMS_OUTPUT.PUT_LINE('7a. 1er echec             -> ' || v_message);
    echecConnexion('bob', v_message);
    DBMS_OUTPUT.PUT_LINE('7b. 2e echec              -> ' || v_message);
    echecConnexion('bob', v_message);
    DBMS_OUTPUT.PUT_LINE('7c. 3e echec              -> ' || v_message);

    succesConnexion('bob', v_message);
    DBMS_OUTPUT.PUT_LINE('7d. Connexion pendant blocage -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- ESSAI 8 : premiere connexion -> changement de mot de passe impose
--           (relation <<extend>> {1re connexion})
-- ---------------------------------------------------------------------
DECLARE
    v_message VARCHAR2(200);
BEGIN
    succesConnexion('chloe', v_message);
    DBMS_OUTPUT.PUT_LINE('8. 1re connexion de Chloe -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- ESSAI 9 : pas de resultat avant la cloture, puis depouillement
-- ---------------------------------------------------------------------
DECLARE
    v_message VARCHAR2(200);
BEGIN
    publierResultat('R1', 1, v_message);
    DBMS_OUTPUT.PUT_LINE('9a. Publier avant cloture -> ' || v_message);

    cloturerReferendum('R1', v_message);
    DBMS_OUTPUT.PUT_LINE('9b. Cloturer R1           -> ' || v_message);

    publierResultat('R1', 99, v_message);
    DBMS_OUTPUT.PUT_LINE('9c. 99 oui pour 2 bulletins -> ' || v_message);

    publierResultat('R1', 1, v_message);
    DBMS_OUTPUT.PUT_LINE('9d. Publier 1 oui         -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- ESSAI 10 : bulletin arrive apres la fin du scrutin.
-- Le sujet exige explicitement un message d'erreur dans ce cas.
-- R1 etant deja depouille, on rejoue la situation sur R2.
-- ---------------------------------------------------------------------
DECLARE
    v_message VARCHAR2(200);
BEGIN
    INSERT INTO UrnesChiffrees
      (idReferendum, idScrutateur, clePubliqueP, clePubliqueG, clePubliqueH)
    VALUES ('R2', 'U2', '23', '2', '8');
    COMMIT;

    inscrireElecteur('R2', 'U5', v_message);
    DBMS_OUTPUT.PUT_LINE('10a. Inscrire Chloe a R2  -> ' || v_message);

    UPDATE Referendums
    SET dateOuverture = SYSDATE - 10,
        dateFermeture = SYSDATE - 1
    WHERE idReferendum = 'R2';
    COMMIT;

    lancerReferendum('R2', v_message);
    DBMS_OUTPUT.PUT_LINE('10b. Lancer R2            -> ' || v_message);

    enregistrerVote('R2', 'U5', '2', '7', v_message);
    DBMS_OUTPUT.PUT_LINE('10c. Chloe vote trop tard -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- ESSAI 11 : gerer les electeurs
--   identifiant deja pris -> refuse (scenario A1)
-- ---------------------------------------------------------------------
DECLARE
    v_message VARCHAR2(200);
BEGIN
    ajouterElecteur('U7', 'emma', 'hash_a_remplacer', 'Blanc', 'Emma', v_message);
    DBMS_OUTPUT.PUT_LINE('11a. Ajouter Emma         -> ' || v_message);

    ajouterElecteur('U8', 'emma', 'hash_a_remplacer', 'Roy', 'Elsa', v_message);
    DBMS_OUTPUT.PUT_LINE('11b. Identifiant en double -> ' || v_message);

    modifierElecteur('U7', 'emma.blanc', 'Blanc-Roy', 'Emma', v_message);
    DBMS_OUTPUT.PUT_LINE('11c. Modifier Emma        -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- ESSAI 12 : gerer les referendums (scenario A2)
--   un referendum EN_PREPARATION se modifie et se supprime
--   un referendum deja lance, non
-- ---------------------------------------------------------------------
DECLARE
    v_message VARCHAR2(200);
BEGIN
    creerReferendum('R3', 'Horaires',
        'Souhaitez-vous des horaires flexibles ?',
        SYSDATE + 1, SYSDATE + 10, 'U1', v_message);
    DBMS_OUTPUT.PUT_LINE('12a. Creer R3             -> ' || v_message);

    creerReferendum('R4', 'Dates incoherentes', 'Question ?',
        SYSDATE + 10, SYSDATE + 1, 'U1', v_message);
    DBMS_OUTPUT.PUT_LINE('12b. Dates a l''envers     -> ' || v_message);

    creerReferendum('R5', 'Par un electeur', 'Question ?',
        SYSDATE + 1, SYSDATE + 10, 'U4', v_message);
    DBMS_OUTPUT.PUT_LINE('12c. Cree par un electeur -> ' || v_message);

    modifierReferendum('R3', 'Horaires flexibles',
        'Souhaitez-vous des horaires flexibles toute l''annee ?',
        SYSDATE + 1, SYSDATE + 15, v_message);
    DBMS_OUTPUT.PUT_LINE('12d. Modifier R3 (prepa)  -> ' || v_message);

    modifierReferendum('R1', 'Nouveau titre', 'Nouvelle question ?',
        SYSDATE, SYSDATE + 5, v_message);
    DBMS_OUTPUT.PUT_LINE('12e. Modifier R1 (lance)  -> ' || v_message);

    supprimerReferendum('R1', v_message);
    DBMS_OUTPUT.PUT_LINE('12f. Supprimer R1 (lance) -> ' || v_message);

    supprimerReferendum('R3', v_message);
    DBMS_OUTPUT.PUT_LINE('12g. Supprimer R3 (prepa) -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- AFFICHAGE FINAL
-- ---------------------------------------------------------------------
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Etat des referendums ---');
    affichageTousReferendums;
    DBMS_OUTPUT.PUT_LINE('--- Resultat de R1 ---');
    affichageResultat('R1');
    DBMS_OUTPUT.PUT_LINE('--- Resultat de R2 (non depouille) ---');
    affichageResultat('R2');
END;
/

-- La vue publique ne montre que les referendums depouilles
SELECT * FROM ResultatsPublics;
