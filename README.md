# SAE_3.01 Système de vote électronique sécurisé

Ce projet consiste à développer un **système de vote électronique sécurisé** permettant de créer et gérer des référendums en ligne au sein d'une entreprise.

L'application repose sur une architecture **client-serveur en Java**, utilisant des sockets pour les communications. La confidentialité des votes est assurée grâce au **chiffrement ElGamal**, permettant d'agréger les bulletins chiffrés sans avoir à les déchiffrer individuellement.

Le système permet notamment de :

- Gérer les électeurs et les référendums.
- Authentifier les utilisateurs selon leur rôle.
- Lancer, clôturer et dépouiller un référendum.
- Chiffrer et enregistrer les votes.
- Calculer et publier les résultats.
- Garantir la confidentialité et l'intégrité des votes.

## Configuration du serveur (poivre des mots de passe)

Les mots de passe sont hachés côté serveur avec la méthode demandé par le client, puis bcrypt (voir `MotDePasse`). Le poivre ne doit jamais être écrit dans le code ni poussé sur GitHub, à la place on le lit dans la variable d'environnement `SAE_POIVRE`, à définir avant de lancer le serveur.

```bash
openssl rand -base64 18        # génère un poivre (à faire une seule fois)
export SAE_POIVRE="le_résultat_de_la_commande"
```

Les tests unitaires de `MotDePasse` n'en ont pas besoin.

### Changer le poivre

À faire si le poivre a fuité (poussé sur GitHub par erreur, copié sur une machine non sûre…). On ne peut pas re-hacher une empreinte sans le mot de passe en clair : chaque compte est donc mis à jour **au moment où son propriétaire se connecte**.

1. Générer un nouveau poivre : `openssl rand -base64 18`.
2. Arrêter le serveur, puis le relancer avec les deux poivres :
   ```bash
   export SAE_POIVRE_ANCIEN="l_ancien_poivre"
   export SAE_POIVRE="le_nouveau_poivre"
   ```
3. Pendant la transition, à chaque connexion réussie avec l'ancien poivre, le serveur refait l'empreinte avec le nouveau (message `Empreinte de ... refaite avec le nouveau poivre` dans ses logs). Les utilisateurs ne voient rien.
4. Fin de la transition : retirer `SAE_POIVRE_ANCIEN` et relancer le serveur. Les comptes qui ne se sont pas connectés entre-temps ne peuvent plus se connecter : l'organisateur leur redonne un mot de passe provisoire (ils devront le changer).

Sans `SAE_POIVRE_ANCIEN`, changer `SAE_POIVRE` rend toutes les empreintes illisibles d'un coup. Si le poivre **et** la base ont fuité ensemble, ne pas faire de transition : redonner directement un mot de passe provisoire à tout le monde.

## Démarrage du sprint 1

- [Schéma et règles de la base de données](bdd/README.md)
- [Arborescence Java proposée et revue des diagrammes de séquence système](docs/architecture/arborescence-et-dss.md)

Le dépôt contient encore une maquette HTML de connexion. Le cœur demandé par le sujet reste constitué de trois programmes Java : client électeur, urne serveur et scrutateur.
