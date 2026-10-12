-- =====================================================================
-- SAE 3.01 - Vote electronique - Sprint 1
-- Fonctions et procedures stockees (PL/SQL, cours BD3 dossier 1)
-- A executer APRES 01_creation_tables.sql
-- =====================================================================
-- Les regles metier que CHECK et FOREIGN KEY ne savent pas exprimer sont
-- ici, dans des sous-programmes stockes. Le programme Java les appelle
-- par JDBC au lieu de refaire les verifications lui-meme.
--
-- Convention : chaque procedure renvoie un parametre OUT p_message.
--   - p_message = 'OK'       -> l'operation a ete faite
--   - p_message = autre      -> refus, le texte explique pourquoi
-- C'est ce message que le client Java affiche a l'utilisateur.

SET SERVEROUTPUT ON;


-- =====================================================================
-- 1. FONCTIONS
-- =====================================================================

-- Nombre d'electeurs inscrits a un referendum.
CREATE OR REPLACE FUNCTION nbElecteursInscrits
    (p_idReferendum IN Referendums.idReferendum%TYPE)
RETURN NUMBER IS
    v_nbInscrits NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_nbInscrits
    FROM Inscriptions
    WHERE idReferendum = p_idReferendum;

    RETURN v_nbInscrits;
END;
/
SHOW ERRORS;


-- Nombre d'electeurs ayant deja vote a un referendum.
CREATE OR REPLACE FUNCTION nbVotants
    (p_idReferendum IN Referendums.idReferendum%TYPE)
RETURN NUMBER IS
    v_nbVotants NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_nbVotants
    FROM Inscriptions
    WHERE idReferendum = p_idReferendum
      AND dateVote IS NOT NULL;

    RETURN v_nbVotants;
END;
/
SHOW ERRORS;


-- Renvoie 1 si l'electeur a deja vote a ce referendum, 0 sinon.
-- Renvoie NULL s'il n'y est pas inscrit.
CREATE OR REPLACE FUNCTION aDejaVote
    (p_idReferendum IN Referendums.idReferendum%TYPE,
     p_idUtilisateur IN Electeurs.idUtilisateur%TYPE)
RETURN NUMBER IS
    v_dateVote Inscriptions.dateVote%TYPE;
BEGIN
    SELECT dateVote INTO v_dateVote
    FROM Inscriptions
    WHERE idReferendum = p_idReferendum
      AND idUtilisateur = p_idUtilisateur;

    IF v_dateVote IS NULL THEN
        RETURN 0;
    ELSE
        RETURN 1;
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN NULL;
END;
/
SHOW ERRORS;


-- =====================================================================
-- 2. AUTHENTIFICATION (cas S'authentifier)
-- =====================================================================

-- Scenario A2 : apres 3 echecs consecutifs, le compte est bloque 15 min.
CREATE OR REPLACE PROCEDURE echecConnexion
    (p_identifiant IN Utilisateurs.identifiantUtilisateur%TYPE,
     p_message OUT VARCHAR2) IS
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
        -- Meme message que ci-dessus : ne pas reveler qu'un compte existe.
        p_message := 'Identifiant ou mot de passe incorrect';
END;
/
SHOW ERRORS;


-- Remet le compteur a zero apres une connexion reussie.
CREATE OR REPLACE PROCEDURE succesConnexion
    (p_identifiant IN Utilisateurs.identifiantUtilisateur%TYPE,
     p_message OUT VARCHAR2) IS
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

        -- Relation <<extend>> {1re connexion} du diagramme de cas d'utilisation
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
-- 3. GERER LES ELECTEURS
-- =====================================================================

-- Story "Ajouter un electeur". Cree le compte ET la fiche electeur
-- ensemble : soit les deux, soit aucun des deux.
-- Scenario A1 : l'identifiant est deja pris -> l'ajout est refuse.
CREATE OR REPLACE PROCEDURE ajouterElecteur
    (p_idUtilisateur IN Utilisateurs.idUtilisateur%TYPE,
     p_identifiant   IN Utilisateurs.identifiantUtilisateur%TYPE,
     p_mdpHash       IN Utilisateurs.mdpHashUtilisateur%TYPE,
     p_nom           IN Electeurs.nomElecteur%TYPE,
     p_prenom        IN Electeurs.prenomElecteur%TYPE,
     p_message       OUT VARCHAR2) IS
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


-- Story "Modifier un electeur". Scenario A1 : identifiant deja pris.
CREATE OR REPLACE PROCEDURE modifierElecteur
    (p_idUtilisateur IN Electeurs.idUtilisateur%TYPE,
     p_identifiant   IN Utilisateurs.identifiantUtilisateur%TYPE,
     p_nom           IN Electeurs.nomElecteur%TYPE,
     p_prenom        IN Electeurs.prenomElecteur%TYPE,
     p_message       OUT VARCHAR2) IS
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


-- Scenario A3 : la suppression est refusee si l'electeur a deja vote.
-- Dans ce cas on desactive le compte pour conserver l'historique.
CREATE OR REPLACE PROCEDURE supprimerElecteur
    (p_idUtilisateur IN Electeurs.idUtilisateur%TYPE,
     p_message OUT VARCHAR2) IS
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


-- Inscrire un electeur a un referendum, avant la cloture seulement.
CREATE OR REPLACE PROCEDURE inscrireElecteur
    (p_idReferendum IN Referendums.idReferendum%TYPE,
     p_idUtilisateur IN Electeurs.idUtilisateur%TYPE,
     p_message OUT VARCHAR2) IS
    v_statut Referendums.statutReferendum%TYPE;
BEGIN
    SELECT statutReferendum INTO v_statut
    FROM Referendums
    WHERE idReferendum = p_idReferendum;

    IF v_statut IN ('CLOTURE', 'DEPOUILLE') THEN
        p_message := 'Referendum cloture : inscription impossible';
    ELSE
        INSERT INTO Inscriptions (idReferendum, idUtilisateur)
        VALUES (p_idReferendum, p_idUtilisateur);
        COMMIT;
        p_message := 'OK';
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_message := 'Ce referendum n''existe pas';
    WHEN DUP_VAL_ON_INDEX THEN
        p_message := 'Cet electeur est deja inscrit';
    WHEN OTHERS THEN
        ROLLBACK;
        p_message := 'Inscription impossible';
END;
/
SHOW ERRORS;


-- =====================================================================
-- 4. GERER LES REFERENDUMS
-- =====================================================================

-- Story "Creer un referendum". Scenario A1 : donnees invalides.
CREATE OR REPLACE PROCEDURE creerReferendum
    (p_idReferendum   IN Referendums.idReferendum%TYPE,
     p_titre          IN Referendums.titreReferendum%TYPE,
     p_question       IN Referendums.questionReferendum%TYPE,
     p_dateOuverture  IN Referendums.dateOuverture%TYPE,
     p_dateFermeture  IN Referendums.dateFermeture%TYPE,
     p_idOrganisateur IN Referendums.idOrganisateur%TYPE,
     p_message        OUT VARCHAR2) IS
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


-- Story "Modifier un referendum".
-- Scenario A2 : le referendum est deja lance, la modification est refusee.
CREATE OR REPLACE PROCEDURE modifierReferendum
    (p_idReferendum  IN Referendums.idReferendum%TYPE,
     p_titre         IN Referendums.titreReferendum%TYPE,
     p_question      IN Referendums.questionReferendum%TYPE,
     p_dateOuverture IN Referendums.dateOuverture%TYPE,
     p_dateFermeture IN Referendums.dateFermeture%TYPE,
     p_message       OUT VARCHAR2) IS
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


-- Story "Supprimer un referendum".
-- Scenario A2 : le referendum est deja lance, la suppression est refusee.
-- Un referendum en preparation n'a ni vote ni resultat : on peut donc
-- effacer ses inscriptions et sa cle publique avec lui.
CREATE OR REPLACE PROCEDURE supprimerReferendum
    (p_idReferendum IN Referendums.idReferendum%TYPE,
     p_message      OUT VARCHAR2) IS
    v_statut Referendums.statutReferendum%TYPE;
BEGIN
    SELECT statutReferendum INTO v_statut
    FROM Referendums
    WHERE idReferendum = p_idReferendum;

    IF v_statut <> 'EN_PREPARATION' THEN
        p_message := 'Referendum deja lance : suppression refusee';
    ELSE
        DELETE FROM UrnesChiffrees WHERE idReferendum = p_idReferendum;
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


-- =====================================================================
-- 5. LANCER ET CLOTURER LE REFERENDUM
-- =====================================================================

-- Scenario A1 du cas Lancer : le lancement est refuse si la
-- configuration est incomplete, et le systeme dit ce qui manque.
CREATE OR REPLACE PROCEDURE lancerReferendum
    (p_idReferendum IN Referendums.idReferendum%TYPE,
     p_message OUT VARCHAR2) IS
    v_statut   Referendums.statutReferendum%TYPE;
    v_nbCles   NUMBER;
    v_nbInscrits NUMBER;
BEGIN
    SELECT statutReferendum INTO v_statut
    FROM Referendums
    WHERE idReferendum = p_idReferendum;

    SELECT COUNT(*) INTO v_nbCles
    FROM UrnesChiffrees
    WHERE idReferendum = p_idReferendum;

    v_nbInscrits := nbElecteursInscrits(p_idReferendum);

    IF v_statut <> 'EN_PREPARATION' THEN
        p_message := 'Referendum deja lance ou cloture';
    ELSIF v_nbCles = 0 THEN
        p_message := 'Il manque la cle publique du scrutateur';
    ELSIF v_nbInscrits = 0 THEN
        p_message := 'Aucun electeur inscrit';
    ELSE
        UPDATE Referendums
        SET statutReferendum = 'OUVERT'
        WHERE idReferendum = p_idReferendum;
        COMMIT;
        p_message := 'OK';
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_message := 'Ce referendum n''existe pas';
END;
/
SHOW ERRORS;


CREATE OR REPLACE PROCEDURE cloturerReferendum
    (p_idReferendum IN Referendums.idReferendum%TYPE,
     p_message OUT VARCHAR2) IS
    v_statut Referendums.statutReferendum%TYPE;
BEGIN
    SELECT statutReferendum INTO v_statut
    FROM Referendums
    WHERE idReferendum = p_idReferendum;

    IF v_statut <> 'OUVERT' THEN
        p_message := 'Le referendum n''est pas ouvert';
    ELSE
        UPDATE Referendums
        SET statutReferendum = 'CLOTURE'
        WHERE idReferendum = p_idReferendum;
        COMMIT;
        p_message := 'OK';
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_message := 'Ce referendum n''existe pas';
END;
/
SHOW ERRORS;


-- =====================================================================
-- 6. VOTER - la procedure la plus importante
-- =====================================================================
-- Le chiffrement et l'agregation se font en JAVA avec BigInteger :
-- Oracle ne sait pas multiplier des nombres de 512 bits modulo p.
-- Java calcule le nouvel agregat (u, v) puis appelle cette procedure.
-- Elle verifie tout, puis enregistre le vote ET l'agregat ENSEMBLE,
-- dans une seule transaction : soit les deux, soit aucun des deux.
CREATE OR REPLACE PROCEDURE enregistrerVote
    (p_idReferendum  IN Referendums.idReferendum%TYPE,
     p_idUtilisateur IN Electeurs.idUtilisateur%TYPE,
     p_nouveauU      IN UrnesChiffrees.agregatU%TYPE,
     p_nouveauV      IN UrnesChiffrees.agregatV%TYPE,
     p_message       OUT VARCHAR2) IS
    rty_referendum Referendums%ROWTYPE;
    v_dejaVote     NUMBER;
BEGIN
    SELECT * INTO rty_referendum
    FROM Referendums
    WHERE idReferendum = p_idReferendum;

    v_dejaVote := aDejaVote(p_idReferendum, p_idUtilisateur);

    IF rty_referendum.statutReferendum <> 'OUVERT' THEN
        p_message := 'Le vote n''est pas ouvert';
    ELSIF SYSDATE < rty_referendum.dateOuverture THEN
        p_message := 'Le vote n''a pas encore commence';
    ELSIF SYSDATE > rty_referendum.dateFermeture THEN
        -- Message d'erreur exige par le sujet pour les bulletins en retard
        p_message := 'Bulletin arrive apres la cloture : refuse';
    ELSIF v_dejaVote IS NULL THEN
        p_message := 'Vous n''etes pas inscrit a ce referendum';
    ELSIF v_dejaVote = 1 THEN
        p_message := 'Vous avez deja vote';
    ELSE
        UPDATE Inscriptions
        SET dateVote = SYSDATE
        WHERE idReferendum = p_idReferendum
          AND idUtilisateur = p_idUtilisateur;

        UPDATE UrnesChiffrees
        SET agregatU = p_nouveauU,
            agregatV = p_nouveauV,
            nbVotesRecus = nbVotesRecus + 1
        WHERE idReferendum = p_idReferendum;

        COMMIT;
        p_message := 'OK';
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_message := 'Ce referendum n''existe pas';
    WHEN OTHERS THEN
        ROLLBACK;
        p_message := 'Vote non enregistre';
END;
/
SHOW ERRORS;


-- =====================================================================
-- 7. DEPOUILLER ET PUBLIER
-- =====================================================================
-- Le scrutateur a dechiffre l'agregat en Java et renvoie le total des
-- "oui". On refuse la publication tant que l'urne n'est pas fermee,
-- et on verifie que le nombre de bulletins annonce est bien celui que
-- l'urne a compte.
CREATE OR REPLACE PROCEDURE publierResultat
    (p_idReferendum IN Referendums.idReferendum%TYPE,
     p_voixPour     IN Resultats.voixPour%TYPE,
     p_message      OUT VARCHAR2) IS
    v_statut    Referendums.statutReferendum%TYPE;
    v_nbRecus   UrnesChiffrees.nbVotesRecus%TYPE;
BEGIN
    SELECT statutReferendum INTO v_statut
    FROM Referendums
    WHERE idReferendum = p_idReferendum;

    SELECT nbVotesRecus INTO v_nbRecus
    FROM UrnesChiffrees
    WHERE idReferendum = p_idReferendum;

    IF v_statut <> 'CLOTURE' THEN
        p_message := 'Referendum non cloture : depouillement impossible';
    ELSIF p_voixPour > v_nbRecus THEN
        p_message := 'Plus de oui que de bulletins : resultat incoherent';
    ELSE
        INSERT INTO Resultats (idReferendum, voixPour, nbVotesDepouilles)
        VALUES (p_idReferendum, p_voixPour, v_nbRecus);

        UPDATE Referendums
        SET statutReferendum = 'DEPOUILLE'
        WHERE idReferendum = p_idReferendum;

        COMMIT;
        p_message := 'OK';
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_message := 'Referendum ou urne introuvable';
    WHEN DUP_VAL_ON_INDEX THEN
        p_message := 'Ce referendum a deja ete depouille';
    WHEN OTHERS THEN
        ROLLBACK;
        p_message := 'Publication impossible';
END;
/
SHOW ERRORS;


-- =====================================================================
-- 8. CONSULTER LES RESULTATS (affichage pour la demonstration)
-- =====================================================================
CREATE OR REPLACE PROCEDURE affichageResultat
    (p_idReferendum IN Referendums.idReferendum%TYPE) IS
    rty_resultat ResultatsPublics%ROWTYPE;
BEGIN
    SELECT * INTO rty_resultat
    FROM ResultatsPublics
    WHERE idReferendum = p_idReferendum;

    DBMS_OUTPUT.PUT_LINE('Referendum : ' || rty_resultat.titreReferendum);
    DBMS_OUTPUT.PUT_LINE('Question   : ' || rty_resultat.questionReferendum);
    DBMS_OUTPUT.PUT_LINE('OUI        : ' || rty_resultat.voixPour);
    DBMS_OUTPUT.PUT_LINE('NON        : ' || rty_resultat.voixContre);
    DBMS_OUTPUT.PUT_LINE('Votants    : ' || rty_resultat.nbVotesDepouilles);

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('Aucun resultat disponible pour '
                             || p_idReferendum);
END;
/
SHOW ERRORS;


-- Affiche tous les referendums et, pour chacun, l'etat du scrutin.
-- Parcours avec un curseur FOR, comme au TP2.
CREATE OR REPLACE PROCEDURE affichageTousReferendums IS
BEGIN
    FOR rec_ref IN (SELECT idReferendum, titreReferendum, statutReferendum
                    FROM Referendums
                    ORDER BY idReferendum) LOOP

        DBMS_OUTPUT.PUT_LINE(RPAD(rec_ref.idReferendum, 6)
                             || RPAD(rec_ref.titreReferendum, 25)
                             || RPAD(rec_ref.statutReferendum, 16)
                             || nbVotants(rec_ref.idReferendum) || '/'
                             || nbElecteursInscrits(rec_ref.idReferendum)
                             || ' votants');
    END LOOP;
END;
/
SHOW ERRORS;
