# EcoMap Invest — nœud de scoring RMI (Systèmes distribués)

Le **calcul de score de saturation** s’exécute dans une **JVM séparée**. Spring Boot est **client RMI** et expose un REST de démo.

## Démarrer avec Docker (recommandé)

À la **racine du dépôt** :

```bash
docker compose up --build -d
```

- **`rmi-scoring`** : registry **1099** + export objet **45000**, hostname RMI `rmi-scoring` (réseau Docker).
- **`backend`** : attend que RMI soit **healthy** (`nc` sur 1099 et 45000), puis démarre avec `RMI_SCORING_HOST=rmi-scoring`.

Tester depuis ta machine (navigateur / Postman / curl) :

```bash
curl "http://localhost:8080/api/v1/rmi/score?drivers=2&competitors=1&density=0.8"
curl "http://localhost:8080/api/v1/rmi/ping"
```

Arrêter : `docker compose down`

## Architecture

1. **`rmi-scoring-api`** — interface `ScoringRemote` (partagée).
2. **`rmi-scoring-server`** — `ScoringRemoteImpl` + registry.
3. **`backend`** — `RmiScoringClient` + `GET /api/v1/rmi/score` et `GET /api/v1/rmi/ping`.

## Build Maven sans Docker (depuis la racine)

```bash
mvn -pl rmi-scoring-server -am -DskipTests package
mvn -pl backend -am -DskipTests package
```

## Lancer le serveur RMI en local (sans Docker)

```bash
export RMI_REGISTRY_PORT=1099
export RMI_EXPORT_PORT=45000
java -Djava.rmi.server.hostname=localhost \
  -jar rmi-scoring-server/target/rmi-scoring-server-0.0.1-SNAPSHOT.jar
```

Puis Spring : `mvn -pl backend -am spring-boot:run`

Variables : `RMI_SCORING_HOST`, `RMI_SCORING_REGISTRY_PORT`, `RMI_SERVICE_NAME` (voir `application.yml`).

Si le nœud RMI est arrêté, l’API répond **503**.

Phrase soutenance : *« Le calcul de score est déporté sur un processus Java distant exposé en RMI ; l’API REST Spring joue le rôle de client RMI. »*
