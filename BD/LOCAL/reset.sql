-- Remet la base locale a zero : tables + procedures + donnees d'essai.
-- Les erreurs ORA-00942 des DROP TABLE au tout premier lancement sont normales
-- (les tables n'existent pas encore).
SET SERVEROUTPUT ON
@/bd/01_creation_tables.sql
@/bd/02_procedures.sql
@/bd/TEST/03_donnees_essai.sql
@/bd/LOCAL/etat.sql
EXIT
