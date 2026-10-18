-- =====================================================================
-- SAE 3.01 - Vote electronique - Sprint 1

SET SERVEROUTPUT ON;


-- =====================================================================
-- S'AUTHENTIFIER
-- =====================================================================

CREATE OR REPLACE PROCEDURE echecConnexion
    (p_identifiant IN Utilisateurs.identifiantUtilisateur%TYPE,
     p_message OUT VARCHAR) IS
    v_idUtilisateur Utilisateurs.idUtilisateur%TYPE;
    v_tentatives    Utilisateurs.tentativesEchouees%TYPE;
BEGIN
    SELECT idUtilisateur, tentativesEchouees
    INTO v_idUtilisateur, v_tentatives
    FROM Utilisateurs
    WHERE identifiantUtilisateur = p_identifiant;

    v_tentatives := v_tentatives + 1;

    IF v_tentatives >= 3 THEN
        UPDATE Utilisateurs
        SET tentativesEchouees = v_tentatives,
            verrouilleJusqua = SYSDATE + 15/1440
        WHERE idUtilisateur = v_idUtilisateur;
        p_message := 'Compte bloque 15 minutes apres 3 echecs';
    ELSE
        UPDATE Utilisateurs
        SET tentativesEchouees = v_tentatives
        WHERE idUtilisateur = v_idUtilisateur;
        p_message := 'Identifiant ou mot de passe incorrect';
    END IF;

    COMMIT;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_message := 'Identifiant ou mot de passe incorrect';
END;
/
SHOW ERRORS;


CREATE OR REPLACE PROCEDURE succesConnexion
    (p_identifiant IN Utilisateurs.identifiantUtilisateur%TYPE,
     p_message OUT VARCHAR) IS
    rty_utilisateur Utilisateurs%ROWTYPE;
BEGIN
    SELECT * INTO rty_utilisateur
    FROM Utilisateurs
    WHERE identifiantUtilisateur = p_identifiant;

    IF rty_utilisateur.actifUtilisateur = 0 THEN
        p_message := 'Compte desactive';
    ELSIF rty_utilisateur.verrouilleJusqua IS NOT NULL
          AND rty_utilisateur.verrouilleJusqua > SYSDATE THEN
        p_message := 'Compte bloque jusqu''a '
                     || TO_CHAR(rty_utilisateur.verrouilleJusqua, 'HH24:MI');
    ELSE
        UPDATE Utilisateurs
        SET tentativesEchouees = 0,
            verrouilleJusqua = NULL
        WHERE idUtilisateur = rty_utilisateur.idUtilisateur;
        COMMIT;
		
        IF rty_utilisateur.doitChangerMdp = 1 THEN
            p_message := 'CHANGER_MDP';
        ELSE
            p_message := 'OK';
        END IF;
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_message := 'Identifiant ou mot de passe incorrect';
END;
/
SHOW ERRORS;


-- =====================================================================
-- GERER LES ELECTEURS
-- =====================================================================

CREATE OR REPLACE PROCEDURE ajouterElecteur
    (p_idUtilisateur IN Utilisateurs.idUtilisateur%TYPE,
     p_identifiant   IN Utilisateurs.identifiantUtilisateur%TYPE,
     p_mdpHash       IN Utilisateurs.mdpHashUtilisateur%TYPE,
     p_nom           IN Electeurs.nomElecteur%TYPE,
     p_prenom        IN Electeurs.prenomElecteur%TYPE,
     p_message       OUT VARCHAR) IS
BEGIN
    INSERT INTO Utilisateurs
      (idUtilisateur, identifiantUtilisateur, mdpHashUtilisateur, roleUtilisateur)
    VALUES (p_idUtilisateur, p_identifiant, p_mdpHash, 'ELECTEUR');

    INSERT INTO Electeurs (idUtilisateur, nomElecteur, prenomElecteur)
    VALUES (p_idUtilisateur, p_nom, p_prenom);

    COMMIT;
    p_message := 'OK';

EXCEPTION
    WHEN DUP_VAL_ON_INDEX THEN
        ROLLBACK;
        p_message := 'Cet identifiant est deja utilise';
    WHEN OTHERS THEN
        ROLLBACK;
        p_message := 'Ajout impossible';
END;
/
SHOW ERRORS;


CREATE OR REPLACE PROCEDURE modifierElecteur
    (p_idUtilisateur IN Electeurs.idUtilisateur%TYPE,
     p_identifiant   IN Utilisateurs.identifiantUtilisateur%TYPE,
     p_nom           IN Electeurs.nomElecteur%TYPE,
     p_prenom        IN Electeurs.prenomElecteur%TYPE,
     p_message       OUT VARCHAR) IS
    v_nbElecteurs NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_nbElecteurs
    FROM Electeurs
    WHERE idUtilisateur = p_idUtilisateur;

    IF v_nbElecteurs = 0 THEN
        p_message := 'Cet electeur n''existe pas';
    ELSE
        UPDATE Utilisateurs
        SET identifiantUtilisateur = p_identifiant
        WHERE idUtilisateur = p_idUtilisateur;

        UPDATE Electeurs
        SET nomElecteur = p_nom,
            prenomElecteur = p_prenom
        WHERE idUtilisateur = p_idUtilisateur;

        COMMIT;
        p_message := 'OK';
    END IF;

EXCEPTION
    WHEN DUP_VAL_ON_INDEX THEN
        ROLLBACK;
        p_message := 'Cet identifiant est deja utilise';
    WHEN OTHERS THEN
        ROLLBACK;
        p_message := 'Modification impossible';
END;
/
SHOW ERRORS;


CREATE OR REPLACE PROCEDURE supprimerElecteur
    (p_idUtilisateur IN Electeurs.idUtilisateur%TYPE,
     p_message OUT VARCHAR) IS
    v_nbVotesEmis NUMBER;
    v_nomElecteur Electeurs.nomElecteur%TYPE;
BEGIN
    SELECT nomElecteur INTO v_nomElecteur
    FROM Electeurs
    WHERE idUtilisateur = p_idUtilisateur;

    SELECT COUNT(*) INTO v_nbVotesEmis
    FROM Inscriptions
    WHERE idUtilisateur = p_idUtilisateur
      AND dateVote IS NOT NULL;

    IF v_nbVotesEmis > 0 THEN
        UPDATE Utilisateurs
        SET actifUtilisateur = 0
        WHERE idUtilisateur = p_idUtilisateur;
        COMMIT;
        p_message := 'Electeur ayant deja vote : compte desactive, '
                     || 'suppression impossible';
    ELSE
        DELETE FROM Inscriptions WHERE idUtilisateur = p_idUtilisateur;
        DELETE FROM Electeurs    WHERE idUtilisateur = p_idUtilisateur;
        DELETE FROM Utilisateurs WHERE idUtilisateur = p_idUtilisateur;
        COMMIT;
        p_message := 'OK';
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_message := 'Cet electeur n''existe pas';
    WHEN OTHERS THEN
        ROLLBACK;
        p_message := 'Suppression impossible';
END;
/
SHOW ERRORS;


-- =====================================================================
-- GERER LES REFERENDUMS
-- =====================================================================

CREATE OR REPLACE PROCEDURE creerReferendum
    (p_idReferendum   IN Referendums.idReferendum%TYPE,
     p_titre          IN Referendums.titreReferendum%TYPE,
     p_question       IN Referendums.questionReferendum%TYPE,
     p_dateOuverture  IN Referendums.dateOuverture%TYPE,
     p_dateFermeture  IN Referendums.dateFermeture%TYPE,
     p_idOrganisateur IN Referendums.idOrganisateur%TYPE,
     p_message        OUT VARCHAR) IS
    v_role Utilisateurs.roleUtilisateur%TYPE;
BEGIN
    SELECT roleUtilisateur INTO v_role
    FROM Utilisateurs
    WHERE idUtilisateur = p_idOrganisateur;

    IF v_role <> 'ORGANISATEUR' THEN
        p_message := 'Seul un organisateur peut creer un referendum';
    ELSIF p_dateOuverture >= p_dateFermeture THEN
        p_message := 'La date de fin doit etre apres la date de debut';
    ELSE
        INSERT INTO Referendums
          (idReferendum, titreReferendum, questionReferendum,
           dateOuverture, dateFermeture, idOrganisateur)
        VALUES (p_idReferendum, p_titre, p_question,
                p_dateOuverture, p_dateFermeture, p_idOrganisateur);
        COMMIT;
        p_message := 'OK';
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_message := 'Cet organisateur n''existe pas';
    WHEN DUP_VAL_ON_INDEX THEN
        ROLLBACK;
        p_message := 'Ce numero de referendum existe deja';
    WHEN OTHERS THEN
        ROLLBACK;
        p_message := 'Creation impossible';
END;
/
SHOW ERRORS;


CREATE OR REPLACE PROCEDURE modifierReferendum
    (p_idReferendum  IN Referendums.idReferendum%TYPE,
     p_titre         IN Referendums.titreReferendum%TYPE,
     p_question      IN Referendums.questionReferendum%TYPE,
     p_dateOuverture IN Referendums.dateOuverture%TYPE,
     p_dateFermeture IN Referendums.dateFermeture%TYPE,
     p_message       OUT VARCHAR) IS
    v_statut Referendums.statutReferendum%TYPE;
BEGIN
    SELECT statutReferendum INTO v_statut
    FROM Referendums
    WHERE idReferendum = p_idReferendum;

    IF v_statut <> 'EN_PREPARATION' THEN
        p_message := 'Referendum deja lance : modification refusee';
    ELSIF p_dateOuverture >= p_dateFermeture THEN
        p_message := 'La date de fin doit etre apres la date de debut';
    ELSE
        UPDATE Referendums
        SET titreReferendum = p_titre,
            questionReferendum = p_question,
            dateOuverture = p_dateOuverture,
            dateFermeture = p_dateFermeture
        WHERE idReferendum = p_idReferendum;
        COMMIT;
        p_message := 'OK';
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_message := 'Ce referendum n''existe pas';
    WHEN OTHERS THEN
        ROLLBACK;
        p_message := 'Modification impossible';
END;
/
SHOW ERRORS;


CREATE OR REPLACE PROCEDURE supprimerReferendum
    (p_idReferendum IN Referendums.idReferendum%TYPE,
     p_message      OUT VARCHAR) IS
    v_statut Referendums.statutReferendum%TYPE;
BEGIN
    SELECT statutReferendum INTO v_statut
    FROM Referendums
    WHERE idReferendum = p_idReferendum;

    IF v_statut <> 'EN_PREPARATION' THEN
        p_message := 'Referendum deja lance : suppression refusee';
    ELSE
        DELETE FROM Inscriptions   WHERE idReferendum = p_idReferendum;
        DELETE FROM Referendums    WHERE idReferendum = p_idReferendum;
        COMMIT;
        p_message := 'OK';
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_message := 'Ce referendum n''existe pas';
    WHEN OTHERS THEN
        ROLLBACK;
        p_message := 'Suppression impossible';
END;
/
SHOW ERRORS;
