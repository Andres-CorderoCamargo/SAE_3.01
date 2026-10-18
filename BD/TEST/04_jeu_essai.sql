-- =====================================================================
-- SAE 3.01 - Vote electronique - Sprint 1

SET SERVEROUTPUT ON;


-- ---------------------------------------------------------------------
-- S'AUTHENTIFIER
-- ---------------------------------------------------------------------

-- Scenario A2 : blocage apres 3 mots de passe errones
DECLARE
    v_message VARCHAR(200);
BEGIN
    echecConnexion('bob', v_message);
    DBMS_OUTPUT.PUT_LINE('1a. 1er echec             -> ' || v_message);
    echecConnexion('bob', v_message);
    DBMS_OUTPUT.PUT_LINE('1b. 2e echec              -> ' || v_message);
    echecConnexion('bob', v_message);
    DBMS_OUTPUT.PUT_LINE('1c. 3e echec              -> ' || v_message);

    succesConnexion('bob', v_message);
    DBMS_OUTPUT.PUT_LINE('1d. Connexion bloquee     -> ' || v_message);
END;
/

DECLARE
    v_message VARCHAR(200);
BEGIN
    succesConnexion('chloe', v_message);
    DBMS_OUTPUT.PUT_LINE('2a. 1re connexion Chloe   -> ' || v_message);

    succesConnexion('gasquet', v_message);
    DBMS_OUTPUT.PUT_LINE('2b. Connexion organisateur-> ' || v_message);

    succesConnexion('inconnu', v_message);
    DBMS_OUTPUT.PUT_LINE('2c. Compte inexistant     -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- GERER LES ELECTEURS
-- ---------------------------------------------------------------------

DECLARE
    v_message VARCHAR(200);
BEGIN
    ajouterElecteur('U7', 'emma', 'hash_a_remplacer', 'Blanc', 'Emma', v_message);
    DBMS_OUTPUT.PUT_LINE('3a. Ajouter Emma          -> ' || v_message);

    ajouterElecteur('U8', 'emma', 'hash_a_remplacer', 'Roy', 'Elsa', v_message);
    DBMS_OUTPUT.PUT_LINE('3b. Identifiant en double -> ' || v_message);

    modifierElecteur('U7', 'emma.blanc', 'Blanc-Roy', 'Emma', v_message);
    DBMS_OUTPUT.PUT_LINE('3c. Modifier Emma         -> ' || v_message);

    modifierElecteur('U99', 'x', 'y', 'z', v_message);
    DBMS_OUTPUT.PUT_LINE('3d. Modifier un inconnu   -> ' || v_message);
END;
/

-- Scenario A3 : la suppression est refusee si l'electeur a deja vote
DECLARE
    v_message VARCHAR(200);
BEGIN
    -- Emma n'a jamais vote : on peut la supprimer
    supprimerElecteur('U7', v_message);
    DBMS_OUTPUT.PUT_LINE('4a. Supprimer Emma        -> ' || v_message);

    -- On simule un vote d'Alice. Elle est deja inscrite a R1 par le
    -- fichier 03, donc on renseigne sa date de vote au lieu d'inserer.
    UPDATE Inscriptions
    SET dateVote = SYSDATE
    WHERE idReferendum = 'R1' AND idUtilisateur = 'U3';
    COMMIT;

    supprimerElecteur('U3', v_message);
    DBMS_OUTPUT.PUT_LINE('4b. Supprimer Alice       -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- GERER LES REFERENDUMS
-- ---------------------------------------------------------------------

DECLARE
    v_message VARCHAR(200);
BEGIN
    creerReferendum('R3', 'Horaires',
        'Souhaitez-vous des horaires flexibles ?',
        SYSDATE + 1, SYSDATE + 10, 'U1', v_message);
    DBMS_OUTPUT.PUT_LINE('5a. Creer R3              -> ' || v_message);

    creerReferendum('R4', 'Dates incoherentes', 'Question ?',
        SYSDATE + 10, SYSDATE + 1, 'U1', v_message);
    DBMS_OUTPUT.PUT_LINE('5b. Dates a l''envers      -> ' || v_message);

    creerReferendum('R5', 'Par un electeur', 'Question ?',
        SYSDATE + 1, SYSDATE + 10, 'U4', v_message);
    DBMS_OUTPUT.PUT_LINE('5c. Cree par un electeur  -> ' || v_message);
END;
/

-- Scenario A2 : un referendum lance ne se modifie ni ne se supprime
DECLARE
    v_message VARCHAR(200);
BEGIN
    modifierReferendum('R3', 'Horaires flexibles',
        'Souhaitez-vous des horaires flexibles toute l''annee ?',
        SYSDATE + 1, SYSDATE + 15, v_message);
    DBMS_OUTPUT.PUT_LINE('6a. Modifier R3 (prepa)   -> ' || v_message);

    -- On ouvre R1 a la main (lancerReferendum arrive au sprint 2)
    UPDATE Referendums SET statutReferendum = 'OUVERT' WHERE idReferendum = 'R1';
    COMMIT;

    modifierReferendum('R1', 'Nouveau titre', 'Nouvelle question ?',
        SYSDATE, SYSDATE + 5, v_message);
    DBMS_OUTPUT.PUT_LINE('6b. Modifier R1 (lance)   -> ' || v_message);

    supprimerReferendum('R1', v_message);
    DBMS_OUTPUT.PUT_LINE('6c. Supprimer R1 (lance)  -> ' || v_message);

    supprimerReferendum('R3', v_message);
    DBMS_OUTPUT.PUT_LINE('6d. Supprimer R3 (prepa)  -> ' || v_message);
END;
/


-- ---------------------------------------------------------------------
-- ETAT FINAL
-- ---------------------------------------------------------------------
SELECT idUtilisateur, identifiantUtilisateur, roleUtilisateur, actifUtilisateur
FROM Utilisateurs ORDER BY idUtilisateur;

SELECT idReferendum, titreReferendum, statutReferendum
FROM Referendums ORDER BY idReferendum;