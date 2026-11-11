# A "sourcer" dans chaque terminal : source BD/LOCAL/env.sh
export ORACLE_URL="jdbc:oracle:thin:@//localhost:1521/FREEPDB1"
export ORACLE_USER="sae"
export ORACLE_PASSWORD="sae_local"
# Poivre de TEST uniquement (jamais le vrai). Si tu le changes, relance
# l'etape "hacher les comptes d'essai".
export SAE_POIVRE="poivre-de-test-local"

# Raccourci : sae_sql <fichier.sql dans le conteneur>
sae_sql() {
  docker exec -i sae-oracle sqlplus -s sae/sae_local@//localhost:1521/FREEPDB1 @"$1"
}
