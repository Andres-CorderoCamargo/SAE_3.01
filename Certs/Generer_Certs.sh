#!/bin/bash
# Génère un certificat de développement. À lancer une fois par machine.


cd "$(dirname "$0")"



trouver_keytool() {
    # 1. Dans le PATH (Linux/macOS, ou WSL avec le PATH Windows)
    for nom in keytool keytool.exe; do
        if command -v "$nom" >/dev/null 2>&1; then command -v "$nom"; return 0; fi
    done

    # 2. JAVA_HOME
    for nom in keytool keytool.exe; do
        if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/$nom" ]; then
            echo "$JAVA_HOME/bin/$nom"; return 0
        fi
    done

    for dossier in "$HOME/.jdks"/*/bin /mnt/c/Users/*/.jdks/*/bin; do
            for nom in keytool keytool.exe; do
                if [ -x "$dossier/$nom" ]; then echo "$dossier/$nom"; return 0; fi
            done
    done

    return 1
}

KEYTOOL="${KEYTOOL:-$(trouver_keytool)}"

if [ -z "$KEYTOOL" ]; then
    echo "keytool introuvable. Installez un JDK, définissez JAVA_HOME, ou lancez :"
    echo "  KEYTOOL=/chemin/vers/keytool ./Generer_Certs.sh"
    exit 1
fi


echo "keytool utilisé : $KEYTOOL"

read -s -p "Mot de passe du keystore (6 caractères minimum) : " MDP; echo


rm -f serveur.p12 serveur.crt client-truststore.p12


"$KEYTOOL" -genkeypair -alias urne -keyalg RSA -keysize 2048 -validity 365 \
  -storetype PKCS12 -keystore serveur.p12 -storepass "$MDP" \
  -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1" || exit 1

"$KEYTOOL" -exportcert -alias urne -keystore serveur.p12 -storepass "$MDP" \
  -file serveur.crt || exit 1

"$KEYTOOL" -importcert -noprompt -alias urne -file serveur.crt \
  -keystore client-truststore.p12 -storetype PKCS12 -storepass "$MDP" || exit 1

echo "password=$MDP" > tls.properties
echo "Terminé."
ls -l *.p12 *.crt