# Réveil musical

Service qui réveille chaque utilisateur avec un morceau choisi selon la météo du jour, puis le prévient sur son canal préféré (email, SMS ou push — simulés par des mocks, aucun envoi réel).

## Appel principal

`POST /api/v1/alarms/trigger`

```json
{"userId": "alice", "dayOfWeek": "MONDAY", "weather": "SOLEIL"}
```

`weather` ∈ `SOLEIL | PLUIE | NEIGE | NUAGEUX`, `dayOfWeek` ∈ `MONDAY … SUNDAY` (l'ordonnancement n'est pas codé : c'est l'appelant qui déclenche à la bonne heure).

Réponse `200` :

```json
{"userId": "alice", "track": {"title": "Walking On Sunshine", "artist": "Katrina and the Waves"}, "channel": "EMAIL", "degraded": false}
```

`degraded=true` signale un mode dégradé : morceau de secours local et/ou canal de repli. Erreurs : `400` (entrée invalide), `404` (utilisateur inconnu), `503` (aucun canal n'a pu délivrer le réveil — jamais de silence).

Utilisateurs de démonstration (mock du service interne) : `alice` (email), `bob` (SMS), `carol` (push).

## Architecture : besoins métier → décisions techniques

| Besoin métier | Traduction technique |
|---|---|
| Tester / changer de fournisseur musical vite | Port `MusicProvider` ; un adaptateur par fournisseur (`ITunesMusicProvider`, `MusicBrainzMusicProvider`) ; choix et **ordre** pilotés par `app.music.providers`, sans recompilation. Ajouter un fournisseur = une classe `@Component("nom") @Qualifier("source")`. |
| Plusieurs canaux, d'autres à venir | Port `Notifier` ; un adaptateur par canal au-dessus de trois faux « SDK » aux interfaces volontairement différentes (`EmailClient`, `SmsGateway`, `PushService`). Ajouter un canal = un `Notifier` + une valeur de `ChannelType`. |
| Licences et fraîcheur vérifiées | Audit automatisé + gate CI (voir plus bas). |
| Jamais de silence | Chaîne de fournisseurs avec repli (`FailoverMusicProvider`) → liste locale de 7 morceaux (`LocalFallbackTrackSource`, un par jour) ; repli de canal (`NotificationDispatcher`, ordre `app.notification.fallback-order`) ; timeouts HTTP (`app.http.*`) ; échec total des canaux = `503` explicite et loggé. |
| Quota iTunes (~20 req/min) | Cache en décorateur `@Primary` (`CachingMusicProvider` + port `TrackCache`), remplaçable (Redis, Caffeine…) sans toucher au métier. |

Organisation (`fr.cours.musical.alarm.clock.api`) : `domain` (modèle, ports, exceptions — aucune dépendance framework) → `application` (`AlarmService`, `TrackSelector`, `NotificationDispatcher`) → `infra` (`in` : REST ; `out` : adaptateurs ; `config` : câblage).

Les deux exigences d'architecture sont **vérifiées automatiquement** par `ArchitectureTest` (ArchUnit) : le domaine et l'application ne dépendent d'aucun framework, fournisseur ou canal ; les adaptateurs ne se connaissent pas entre eux ; aucun bean Spring n'est instancié avec `new` (IoC / DI). Les champs propres aux API (par ex. `trackViewUrl` d'iTunes) restent dans des DTO privés de l'adaptateur.

Hypothèses : le jour de la semaine sert à choisir le morceau de la liste locale (déterministe) et au texte du message ; les destinataires des mocks sont dérivés de l'`userId` ; la panne du service utilisateur n'est pas couverte.

## Configuration

| Variable | Rôle | Défaut |
|---|---|---|
| `MUSIC_PROVIDERS` | Fournisseurs essayés, dans l'ordre (`itunes`, `musicbrainz`) ; vide = liste locale seule | `itunes,musicbrainz` |
| `MUSICBRAINZ_CONTACT` | Contact placé dans le `User-Agent` exigé par MusicBrainz (à renseigner, jamais commité) | `contact@example.invalid` |
| `NOTIFICATION_FALLBACK_ORDER` | Canaux essayés après le canal préféré | `PUSH,SMS,EMAIL` |
| `app.http.connect-timeout` / `read-timeout` | Timeouts des appels sortants | `2s` / `3s` |

Voir `.env.example`.

## Lancer et tester

```bash
mvn spring-boot:run   # démarre sur http://localhost:8080/api/v1
mvn verify            # tests + seuil JaCoCo (85 % de lignes) ; rapport : target/site/jacoco/index.html
```

La suite couvre : domaine, services (mocks de ports), **tests de contrat** communs à chaque fournisseur musical et à chaque canal, adaptateurs HTTP (réponses simulées), cache, contrôleur REST, bout en bout (panne iTunes → MusicBrainz → liste locale ; canal en panne → repli ; tous canaux en panne → 503), fournisseur qui ne répond jamais (timeout), règles d'architecture.

## Dépendances, licences et fraîcheur

Méthode (reproductible, toutes portées et dépendances transitives comprises) :

```bash
mvn license:download-licenses                  # scan des licences déclarées → target/generated-resources/licenses.xml
python scripts/check_license_whitelist.py      # gate : échoue si licence hors liste blanche (Apache, MIT, BSD, EPL/EDL)
python scripts/dependency_report.py --readme README.md   # régénère le tableau ci-dessous (Maven Central)
```

Le gate tourne en CI (`.github/workflows/ci.yml`, job `license-audit`) ; il a été validé dans les deux sens en ajoutant puis retirant un paquet AGPL de test (`itextpdf`). Le scan brut est conservé dans [`licenses/licenses.xml`](licenses/licenses.xml). « Dernière stable » = plus haute version non pré-release publiée sur Maven Central au moment du scan (2026-10-08).

<!-- DEPS:START -->
| Package | Version installée | Dernière stable | Fraîcheur | Licence(s) |
|---|---|---|---|---|
| ch.qos.logback:logback-classic | 1.5.38 | 1.6.5 | mineure/patch en retard | EPL-2.0 / LGPL-2.1-only |
| ch.qos.logback:logback-core | 1.5.38 | 1.6.5 | mineure/patch en retard | EPL-2.0 / LGPL-2.1-only |
| com.fasterxml.jackson.core:jackson-annotations | 2.21 | 2.22 | mineure/patch en retard | The Apache Software License, Version 2.0 |
| com.fasterxml:classmate | 1.7.3 | 1.7.3 | à jour | Apache License, Version 2.0 |
| com.jayway.jsonpath:json-path | 2.10.0 | 3.0.0 | majeure en retard | The Apache Software License, Version 2.0 |
| com.tngtech.archunit:archunit | 1.5.1 | 1.5.1 | à jour | The Apache Software License, Version 2.0 / BSD |
| com.tngtech.archunit:archunit-junit5 | 1.5.1 | 1.5.1 | à jour | The Apache Software License, Version 2.0 |
| com.tngtech.archunit:archunit-junit5-api | 1.5.1 | 1.5.1 | à jour | The Apache Software License, Version 2.0 |
| com.tngtech.archunit:archunit-junit5-engine | 1.5.1 | 1.5.1 | à jour | The Apache Software License, Version 2.0 |
| com.tngtech.archunit:archunit-junit5-engine-api | 1.5.1 | 1.5.1 | à jour | The Apache Software License, Version 2.0 |
| com.vaadin.external.google:android-json | 0.0.20131108.vaadin1 | 0.0.20131108.vaadin1 | à jour | Apache License 2.0 |
| commons-logging:commons-logging | 1.3.6 | 1.4.0 | mineure/patch en retard | Apache-2.0 |
| io.micrometer:micrometer-commons | 1.17.1 | 1.17.1 | à jour | The Apache Software License, Version 2.0 |
| io.micrometer:micrometer-observation | 1.17.1 | 1.17.1 | à jour | The Apache Software License, Version 2.0 |
| jakarta.activation:jakarta.activation-api | 2.1.4 | 2.1.4 | à jour | EDL 1.0 |
| jakarta.annotation:jakarta.annotation-api | 3.0.0 | 3.0.0 | à jour | EPL 2.0 / GPL2 w/ CPE |
| jakarta.validation:jakarta.validation-api | 3.1.1 | 3.1.1 | à jour | Apache License 2.0 |
| jakarta.xml.bind:jakarta.xml.bind-api | 4.0.5 | 4.0.5 | à jour | Eclipse Distribution License - v 1.0 |
| net.bytebuddy:byte-buddy | 1.18.11 | 1.18.14-jdk5 | mineure/patch en retard | Apache License, Version 2.0 |
| net.bytebuddy:byte-buddy-agent | 1.18.11 | 1.18.14-jdk5 | mineure/patch en retard | Apache License, Version 2.0 |
| net.minidev:accessors-smart | 2.6.0 | 2.6.0 | à jour | The Apache Software License, Version 2.0 |
| net.minidev:json-smart | 2.6.0 | 2.6.0 | à jour | The Apache Software License, Version 2.0 |
| org.apache.logging.log4j:log4j-api | 2.25.5 | 2.26.1 | mineure/patch en retard | Apache-2.0 |
| org.apache.logging.log4j:log4j-to-slf4j | 2.25.5 | 2.26.1 | mineure/patch en retard | Apache-2.0 |
| org.apache.tomcat.embed:tomcat-embed-core | 11.0.24 | 11.0.26 | mineure/patch en retard | Apache License, Version 2.0 |
| org.apache.tomcat.embed:tomcat-embed-el | 11.0.24 | 11.0.26 | mineure/patch en retard | Apache License, Version 2.0 |
| org.apache.tomcat.embed:tomcat-embed-websocket | 11.0.24 | 11.0.26 | mineure/patch en retard | Apache License, Version 2.0 |
| org.apiguardian:apiguardian-api | 1.1.2 | 1.1.2 | à jour | The Apache License, Version 2.0 |
| org.assertj:assertj-core | 3.27.7 | 3.27.7 | à jour | Apache-2.0 |
| org.awaitility:awaitility | 4.3.0 | 4.3.0 | à jour | Apache 2.0 |
| org.hamcrest:hamcrest | 3.0 | 3.0 | à jour | BSD-3-Clause |
| org.hibernate.validator:hibernate-validator | 9.1.3.Final | 9.1.4.Final | mineure/patch en retard | Apache License 2.0 |
| org.jboss.logging:jboss-logging | 3.6.3.Final | 3.6.3.Final | à jour | Apache License 2.0 |
| org.jspecify:jspecify | 1.0.1 | 1.0.1 | à jour | The Apache License, Version 2.0 |
| org.junit.jupiter:junit-jupiter | 6.0.3 | 6.1.3 | mineure/patch en retard | Eclipse Public License v2.0 |
| org.junit.jupiter:junit-jupiter-api | 6.0.3 | 6.1.3 | mineure/patch en retard | Eclipse Public License v2.0 |
| org.junit.jupiter:junit-jupiter-engine | 6.0.3 | 6.1.3 | mineure/patch en retard | Eclipse Public License v2.0 |
| org.junit.jupiter:junit-jupiter-params | 6.0.3 | 6.1.3 | mineure/patch en retard | Eclipse Public License v2.0 |
| org.junit.platform:junit-platform-commons | 6.0.3 | 6.1.3 | mineure/patch en retard | Eclipse Public License v2.0 |
| org.junit.platform:junit-platform-engine | 6.0.3 | 6.1.3 | mineure/patch en retard | Eclipse Public License v2.0 |
| org.mockito:mockito-core | 5.23.0 | 5.24.0 | mineure/patch en retard | MIT |
| org.mockito:mockito-junit-jupiter | 5.23.0 | 5.24.0 | mineure/patch en retard | MIT |
| org.objenesis:objenesis | 3.3 | 3.6 | mineure/patch en retard | Apache License, Version 2.0 |
| org.opentest4j:opentest4j | 1.3.0 | 1.3.0 | à jour | The Apache License, Version 2.0 |
| org.ow2.asm:asm | 9.7.1 | 9.10.1 | mineure/patch en retard | BSD-3-Clause |
| org.projectlombok:lombok | 1.18.46 | 1.18.48 | mineure/patch en retard | The MIT License |
| org.skyscreamer:jsonassert | 1.5.3 | 1.5.3 | à jour | The Apache Software License, Version 2.0 |
| org.slf4j:jul-to-slf4j | 2.0.18 | 2.0.20 | mineure/patch en retard | MIT |
| org.slf4j:slf4j-api | 2.0.18 | 2.0.20 | mineure/patch en retard | MIT |
| org.springframework.boot:spring-boot | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-autoconfigure | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-http-converter | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-jackson | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-resttestclient | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-servlet | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-jackson | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-jackson-test | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-logging | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-test | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-tomcat | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-tomcat-runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-validation | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-web | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-webmvc | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-webmvc-test | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-test | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-test-autoconfigure | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-tomcat | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-validation | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-web-server | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-webmvc | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-webmvc-test | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-aop | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-beans | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-context | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-core | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-expression | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-test | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-web | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-webmvc | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.xmlunit:xmlunit-core | 2.11.0 | 2.14.0 | mineure/patch en retard | The Apache Software License, Version 2.0 |
| org.yaml:snakeyaml | 2.6 | 2.7 | mineure/patch en retard | Apache License, Version 2.0 |
| tools.jackson.core:jackson-core | 3.1.5 | 3.2.3 | mineure/patch en retard | The Apache Software License, Version 2.0 |
| tools.jackson.core:jackson-databind | 3.1.5 | 3.2.3 | mineure/patch en retard | The Apache Software License, Version 2.0 |
<!-- DEPS:END -->

### Outils de build (hors livrable, non redistribués)

| Plugin | Version | Dernière stable | Licence |
|---|---|---|---|
| `org.springframework.boot:spring-boot-maven-plugin` | 4.1.1 | 4.1.1 | Apache-2.0 |
| `org.jacoco:jacoco-maven-plugin` | 0.8.15 | 0.8.15 | EPL-2.0 |
| `org.codehaus.mojo:license-maven-plugin` | 2.7.1 | 2.7.1 | LGPL-3.0 (voir ci-dessous) |
| `org.apache.maven.plugins:maven-surefire-plugin` | 3.5.6 | 3.6.0 (mineure en retard, version imposée par le parent Spring Boot 4.1.1) | Apache-2.0 |

### Composants qui posent question

- **`license-maven-plugin` — LGPL-3.0 (copyleft).** Outil de build uniquement : il n'est ni lié à l'application ni redistribué (absent du jar), donc l'obligation LGPL ne s'applique pas. Il est remplaçable sans toucher au code (le scan ne sert qu'à l'audit).
- **`ch.qos.logback:logback-classic` / `logback-core` — EPL-2.0 / LGPL-2.1.** Double licence : on retient la branche **EPL-2.0** (copyleft faible au niveau fichier, aucune modification ni redistribution du code de Logback). Utilisé uniquement via la façade SLF4J : aucun import `ch.qos.logback` dans `src/`, donc remplaçable sans toucher au métier. Exception relue dans `scripts/check_license_whitelist.py`.
- **`jakarta.annotation:jakarta.annotation-api` — EPL-2.0 / GPL-2.0 avec Classpath Exception.** Double licence : branche **EPL-2.0** élue ; la Classpath Exception supprime de toute façon la propagation du copyleft. Simples annotations consommées par le conteneur Spring. Exception relue dans le script.
- **JUnit 5/6 (`Eclipse Public License v2.0`), `jakarta.xml.bind-api` / `jakarta.activation-api` (EDL 1.0, équivalent BSD-3-Clause).** Portée **test** uniquement (via `spring-boot-starter-test`), jamais dans l'artefact livré. Les orthographes longues de ces licences ont été ajoutées à la liste blanche car elles relèvent des familles déjà approuvées (EPL, BSD).
- **`com.tngtech.archunit:archunit` — Apache-2.0 / BSD.** Portée test (vérification d'architecture) ; version à jour.
- **Versions « en retard » du tableau.** Toutes sont **imposées par le BOM de Spring Boot 4.1.1**, qui est la dernière version stable de Spring Boot au moment du scan ; nous ne les surchargeons pas pour garder l'ensemble testé ensemble par l'équipe Spring (Logback 1.5.x, Jackson annotations 2.21, JUnit 6.0.x, Tomcat 11.0.24…). Les écarts sont des mineures/patchs, sauf `json-path` (majeure, portée test uniquement). Action : relancer le rapport et monter Spring Boot dès la prochaine version corrective, en priorité pour les correctifs Tomcat.

### Services externes consommés (ce ne sont pas des paquets)

- **iTunes Search API** : gratuite, sans clé ; environ 20 requêtes/minute → cache obligatoire côté application (`CachingMusicProvider`). Elle répond en `Content-Type: text/javascript` : l'adaptateur lit le corps en texte puis le désérialise lui-même. Seuls le titre et l'artiste sont conservés (`trackViewUrl` ne sort pas de l'adaptateur).
- **MusicBrainz** : gratuite, sans clé ; **`User-Agent` identifiable obligatoire** (nom, version, contact) sous peine de refus ; limite d'environ 1 requête/seconde par IP au-delà de laquelle les requêtes sont refusées en `503` (règles de limitation de débit publiées par MusicBrainz) → cache, et repli automatique vers la liste locale en cas de refus. Vérifier les conditions d'utilisation et la licence des données avant tout usage commercial.
