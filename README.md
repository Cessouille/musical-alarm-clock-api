# Réveil musical
Célian CHAUSSON - INFRES 17 DL

Service qui réveille chaque utilisateur avec un morceau choisi selon la météo du jour, puis le prévient sur son canal préféré (email, SMS ou push - simulés par des mocks, aucun envoi réel).

## Appel principal

`POST /api/v1/alarms/trigger`

```json
{"userId": "alice", "dayOfWeek": "MONDAY", "weather": "SUN"}
```

`weather` -> `SUN | RAIN | SNOW | CLOUDY`, `dayOfWeek` -> `MONDAY … SUNDAY` (l'ordonnancement n'est pas codé : c'est l'appelant qui déclenche à la bonne heure).

Réponse `200` :

```json
{"userId": "alice", "track": {"title": "Walking On Sunshine", "artist": "Katrina and the Waves"}, "channel": "EMAIL", "degraded": false}
```

`degraded=true` signale un mode dégradé : morceau de secours local et/ou canal de repli. Erreurs : `400` (entrée invalide - `userId` limité à `[A-Za-z0-9._-]`, 64 caractères), `404` (utilisateur inconnu), `503` (aucun canal n'a pu délivrer le réveil, ou service utilisateur indisponible - jamais de faux succès).

Utilisateurs de démonstration (mock du service interne, qui fournit aussi les **coordonnées** `Contact`, une adresse par canal) : `alice` (email ; lundi/mardi × soleil/pluie), `bob` (SMS ; mardi et samedi sous la neige), `carol` (push ; uniquement un morceau de secours).

## Architecture : besoins métier -> décisions techniques

| Besoin métier | Traduction technique |
|---|---|
| Tester / changer de fournisseur musical vite | Port `MusicProvider` ; un adaptateur par fournisseur (`ITunesMusicProvider`, `MusicBrainzMusicProvider`) ; choix et **ordre** pilotés par `app.music.providers`, sans recompilation. Ajouter un fournisseur = une classe `@Component("nom") @Qualifier("source")`. |
| Plusieurs canaux, d'autres à venir | Port `Notifier` ; un adaptateur par canal au-dessus de trois faux « SDK » aux interfaces volontairement différentes (`EmailClient`, `SmsGateway`, `PushService`). Ajouter un canal = un `Notifier` + une valeur de `ChannelType`. |
| Licences et fraîcheur vérifiées | Audit automatisé + gate CI (voir plus bas). |
| Jamais de silence | Chaîne de fournisseurs avec repli (`FailoverMusicProvider`) -> liste locale de 7 morceaux (`LocalFallbackTrackSource`, un par jour) ; repli de canal (`NotificationDispatcher`, ordre `app.notification.fallback-order`) ; timeouts HTTP (`app.http.*`) ; échec total des canaux = `503` explicite et loggé. |
| Quota iTunes (~20 req/min) | Cache en décorateur `@Primary` (`CachingMusicProvider` + port `TrackCache`), remplaçable (Redis, Caffeine…) sans toucher au métier. `InMemoryTrackCache` est borné (`app.cache.max-entries`, LRU) et expire (`app.cache.ttl`). Un fournisseur en échec est mis de côté `app.music.failure-cooldown` (60 s) au lieu d'être re-sollicité à chaque requête. |

Organisation (`fr.cours.musical.alarm.clock.api`) : `domain` (modèle, ports, exceptions - aucune dépendance framework) -> `application` (`AlarmService`, `TrackSelector`, `NotificationDispatcher`) -> `infra` (`in` : REST ; `out` : adaptateurs ; `config` : câblage). Les trois services de `application` portent uniquement l'annotation `@Service` de Spring (plus Lombok/SLF4J) : `ArchitectureTest` leur interdit tout autre accès à Spring (`web`, `http`, `boot`, `beans`, `context`), à Jackson et à `infra`.

Les deux exigences d'architecture sont **vérifiées automatiquement** par `ArchitectureTest` (ArchUnit) : le domaine et l'application ne dépendent d'aucun framework, fournisseur ou canal ; les adaptateurs ne se connaissent pas entre eux (les fournisseurs musicaux sont repérés par l'annotation `@MusicSource`, pas par une chaîne) ; aucun bean Spring n'est instancié avec `new` (IoC / DI). Les champs propres aux API (par ex. `trackViewUrl` d'iTunes) restent dans des DTO privés de l'adaptateur.

**Choix du morceau.** Il dépend du **couple (jour de la semaine, météo)** : le service utilisateur (mocké) renvoie, pour chaque utilisateur, un morceau par combinaison choisie - par exemple lundi + soleil -> *Walking on Sunshine*, lundi + pluie -> *Singin' in the Rain*, mardi + soleil -> *Good Day Sunshine*. Une combinaison non choisie reçoit le **morceau de secours de l'utilisateur** ; si aucun fournisseur musical ne le trouve, la **liste locale** (un morceau par jour) prend le relais.

**Coordonnées et mocks.** Les adresses viennent du service utilisateur (`Contact` = table `ChannelType -> adresse` dans `UserPreferences`), jamais de l'`userId` : le domaine ne connaît aucun champ propre à un canal, donc ajouter WhatsApp ou l'appel vocal ne le modifie pas (seulement une valeur de `ChannelType` et un `Notifier`). Un notifier sans adresse pour son canal échoue explicitement, ce qui déclenche le repli de canal. Les faux SDK `Console*` ne sont enregistrés que si `app.notification.mock=true` (défaut) ; ils écrivent en console le message complet mais avec le destinataire masqué (`al***id`). Le profil `prod` (`application-prod.yaml`) met `mock=false` : l'application **refuse de démarrer** tant qu'un vrai `EmailClient`, `SmsGateway` et `PushService` n'est pas fourni, plutôt que d'annoncer un envoi qui n'a pas eu lieu.

**Panne du service utilisateur.** Sans ses préférences (et donc ses coordonnées), l'utilisateur ne peut pas être joint : la requête échoue en `503` explicite (`UserPreferencesUnavailableException`), loggée en `ERROR`. Un rejeu (file, retry) reste à prévoir côté appelant ou dans une évolution ultérieure, de même que l'idempotence des envois (un repli après un timeout ambigu peut doubler un réveil).

## Configuration

| Variable | Rôle | Défaut |
|---|---|---|
| `MUSIC_PROVIDERS` | Fournisseurs essayés, dans l'ordre (`itunes`, `musicbrainz`) ; vide = liste locale seule | `itunes,musicbrainz` |
| `MUSICBRAINZ_CONTACT` | Contact placé dans le `User-Agent` exigé par MusicBrainz (à renseigner, jamais commité) | `contact@example.invalid` |
| `NOTIFICATION_FALLBACK_ORDER` | Canaux essayés après le canal préféré | `PUSH,SMS,EMAIL` |
| `app.notification.mock` | Active les faux SDK console (`false` sous le profil `prod`) | `true` |
| `app.music.failure-cooldown` | Durée pendant laquelle un fournisseur en échec est ignoré | `60s` |
| `app.cache.ttl` / `max-entries` | Durée de vie et taille maximale du cache de morceaux | `24h` / `10000` |
| `app.http.connect-timeout` / `read-timeout` | Timeouts des appels sortants | `2s` / `3s` |

Sous le profil `prod`, `MUSICBRAINZ_CONTACT` est **obligatoire** (pas de valeur par défaut : le démarrage échoue s'il manque).

Voir `.env.example`.

## Lancer et tester

```bash
mvn spring-boot:run   # démarre sur http://localhost:8080/api/v1
mvn verify            # tests + seuil JaCoCo (85 % de lignes) ; rapport : target/site/jacoco/index.html
```

La suite couvre : domaine, services (mocks de ports), **tests de contrat** communs à chaque fournisseur musical et à chaque canal, adaptateurs HTTP (réponses simulées), cache, contrôleur REST, bout en bout (panne iTunes -> MusicBrainz -> liste locale ; canal en panne -> repli ; tous canaux en panne -> 503), fournisseur qui ne répond jamais (timeout), règles d'architecture.

## Dépendances, licences et fraîcheur

Méthode (reproductible, toutes portées et dépendances transitives comprises) :

```bash
mvn package                                    # produit aussi le SBOM CycloneDX : target/classes/META-INF/sbom/application.cdx.json (hors tests)
mvn license:download-licenses                  # scan des licences déclarées -> target/generated-resources/licenses.xml
python scripts/check_license_whitelist.py      # gate : échoue si licence hors liste blanche (Apache, MIT, BSD, EDL) ou sans exception relue
python scripts/dependency_report.py --readme README.md   # régénère le tableau ci-dessous (Maven Central) ; la portée vient du SBOM
```

Le gate tourne en CI (`.github/workflows/ci.yml`, job `license-audit`) ; il est couvert par des tests automatiques (`scripts/test_check_license_whitelist.py`, exécutés en CI : AGPL/GPL rejetées, licence inconnue rejetée, EPL **non** admise en bloc, exception valable uniquement pour l'artefact et la licence nommés). Le **SBOM** (CycloneDX 1.6) est généré à chaque `mvn package` et embarqué dans le jar. Dependabot (`.github/dependabot.yml`) propose chaque semaine les montées de version Maven et GitHub Actions. Le scan brut est conservé dans [`licenses/licenses.xml`](licenses/licenses.xml). « Dernière stable » = plus haute version non pré-release publiée sur Maven Central au moment du scan (2026-10-08).

<!-- DEPS:START -->
| Package | Portée | Version installée | Dernière stable | Fraîcheur | Licence(s) |
|---|---|---|---|---|---|
| ch.qos.logback:logback-classic | compile/runtime | 1.5.38 | 1.6.5 | mineure/patch en retard | EPL-2.0 / LGPL-2.1-only |
| ch.qos.logback:logback-core | compile/runtime | 1.5.38 | 1.6.5 | mineure/patch en retard | EPL-2.0 / LGPL-2.1-only |
| com.fasterxml.jackson.core:jackson-annotations | compile/runtime | 2.21 | 2.22 | mineure/patch en retard | The Apache Software License, Version 2.0 |
| com.fasterxml:classmate | compile/runtime | 1.7.3 | 1.7.3 | à jour | Apache License, Version 2.0 |
| com.jayway.jsonpath:json-path | test | 2.10.0 | 3.0.0 | majeure en retard | The Apache Software License, Version 2.0 |
| com.tngtech.archunit:archunit | test | 1.5.1 | 1.5.1 | à jour | The Apache Software License, Version 2.0 / BSD |
| com.tngtech.archunit:archunit-junit5 | test | 1.5.1 | 1.5.1 | à jour | The Apache Software License, Version 2.0 |
| com.tngtech.archunit:archunit-junit5-api | test | 1.5.1 | 1.5.1 | à jour | The Apache Software License, Version 2.0 |
| com.tngtech.archunit:archunit-junit5-engine | test | 1.5.1 | 1.5.1 | à jour | The Apache Software License, Version 2.0 |
| com.tngtech.archunit:archunit-junit5-engine-api | test | 1.5.1 | 1.5.1 | à jour | The Apache Software License, Version 2.0 |
| com.vaadin.external.google:android-json | test | 0.0.20131108.vaadin1 | 0.0.20131108.vaadin1 | à jour | Apache License 2.0 |
| commons-logging:commons-logging | compile/runtime | 1.3.6 | 1.4.0 | mineure/patch en retard | Apache-2.0 |
| io.micrometer:micrometer-commons | compile/runtime | 1.17.1 | 1.17.1 | à jour | The Apache Software License, Version 2.0 |
| io.micrometer:micrometer-observation | compile/runtime | 1.17.1 | 1.17.1 | à jour | The Apache Software License, Version 2.0 |
| jakarta.activation:jakarta.activation-api | test | 2.1.4 | 2.1.4 | à jour | EDL 1.0 |
| jakarta.annotation:jakarta.annotation-api | compile/runtime | 3.0.0 | 3.0.0 | à jour | EPL 2.0 / GPL2 w/ CPE |
| jakarta.validation:jakarta.validation-api | compile/runtime | 3.1.1 | 3.1.1 | à jour | Apache License 2.0 |
| jakarta.xml.bind:jakarta.xml.bind-api | test | 4.0.5 | 4.0.5 | à jour | Eclipse Distribution License - v 1.0 |
| net.bytebuddy:byte-buddy | test | 1.18.11 | 1.18.14-jdk5 | mineure/patch en retard | Apache License, Version 2.0 |
| net.bytebuddy:byte-buddy-agent | test | 1.18.11 | 1.18.14-jdk5 | mineure/patch en retard | Apache License, Version 2.0 |
| net.minidev:accessors-smart | test | 2.6.0 | 2.6.0 | à jour | The Apache Software License, Version 2.0 |
| net.minidev:json-smart | test | 2.6.0 | 2.6.0 | à jour | The Apache Software License, Version 2.0 |
| org.apache.logging.log4j:log4j-api | compile/runtime | 2.25.5 | 2.26.1 | mineure/patch en retard | Apache-2.0 |
| org.apache.logging.log4j:log4j-to-slf4j | compile/runtime | 2.25.5 | 2.26.1 | mineure/patch en retard | Apache-2.0 |
| org.apache.tomcat.embed:tomcat-embed-core | compile/runtime | 11.0.26 | 11.0.26 | à jour | Apache License, Version 2.0 |
| org.apache.tomcat.embed:tomcat-embed-el | compile/runtime | 11.0.26 | 11.0.26 | à jour | Apache License, Version 2.0 |
| org.apache.tomcat.embed:tomcat-embed-websocket | compile/runtime | 11.0.26 | 11.0.26 | à jour | Apache License, Version 2.0 |
| org.apiguardian:apiguardian-api | test | 1.1.2 | 1.1.2 | à jour | The Apache License, Version 2.0 |
| org.assertj:assertj-core | test | 3.27.7 | 3.27.7 | à jour | Apache-2.0 |
| org.awaitility:awaitility | test | 4.3.0 | 4.3.0 | à jour | Apache 2.0 |
| org.hamcrest:hamcrest | test | 3.0 | 3.0 | à jour | BSD-3-Clause |
| org.hibernate.validator:hibernate-validator | compile/runtime | 9.1.3.Final | 9.1.4.Final | mineure/patch en retard | Apache License 2.0 |
| org.jboss.logging:jboss-logging | compile/runtime | 3.6.3.Final | 3.6.3.Final | à jour | Apache License 2.0 |
| org.jspecify:jspecify | compile/runtime | 1.0.1 | 1.0.1 | à jour | The Apache License, Version 2.0 |
| org.junit.jupiter:junit-jupiter | test | 6.0.3 | 6.1.3 | mineure/patch en retard | Eclipse Public License v2.0 |
| org.junit.jupiter:junit-jupiter-api | test | 6.0.3 | 6.1.3 | mineure/patch en retard | Eclipse Public License v2.0 |
| org.junit.jupiter:junit-jupiter-engine | test | 6.0.3 | 6.1.3 | mineure/patch en retard | Eclipse Public License v2.0 |
| org.junit.jupiter:junit-jupiter-params | test | 6.0.3 | 6.1.3 | mineure/patch en retard | Eclipse Public License v2.0 |
| org.junit.platform:junit-platform-commons | test | 6.0.3 | 6.1.3 | mineure/patch en retard | Eclipse Public License v2.0 |
| org.junit.platform:junit-platform-engine | test | 6.0.3 | 6.1.3 | mineure/patch en retard | Eclipse Public License v2.0 |
| org.mockito:mockito-core | test | 5.23.0 | 5.24.0 | mineure/patch en retard | MIT |
| org.mockito:mockito-junit-jupiter | test | 5.23.0 | 5.24.0 | mineure/patch en retard | MIT |
| org.objenesis:objenesis | test | 3.3 | 3.6 | mineure/patch en retard | Apache License, Version 2.0 |
| org.opentest4j:opentest4j | test | 1.3.0 | 1.3.0 | à jour | The Apache License, Version 2.0 |
| org.ow2.asm:asm | test | 9.7.1 | 9.10.1 | mineure/patch en retard | BSD-3-Clause |
| org.projectlombok:lombok | compile/runtime | 1.18.46 | 1.18.48 | mineure/patch en retard | The MIT License |
| org.skyscreamer:jsonassert | test | 1.5.3 | 1.5.3 | à jour | The Apache Software License, Version 2.0 |
| org.slf4j:jul-to-slf4j | compile/runtime | 2.0.18 | 2.0.20 | mineure/patch en retard | MIT |
| org.slf4j:slf4j-api | compile/runtime | 2.0.18 | 2.0.20 | mineure/patch en retard | MIT |
| org.springframework.boot:spring-boot | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-autoconfigure | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-http-converter | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-jackson | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-resttestclient | test | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-servlet | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-jackson | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-jackson-test | test | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-logging | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-test | test | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-tomcat | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-tomcat-runtime | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-validation | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-web | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-webmvc | test | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-starter-webmvc-test | test | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-test | test | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-test-autoconfigure | test | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-tomcat | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-validation | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-web-server | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-webmvc | compile/runtime | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework.boot:spring-boot-webmvc-test | test | 4.1.1 | 4.1.1 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-aop | compile/runtime | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-beans | compile/runtime | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-context | compile/runtime | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-core | compile/runtime | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-expression | compile/runtime | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-test | test | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-web | compile/runtime | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.springframework:spring-webmvc | compile/runtime | 7.0.9 | 7.0.9 | à jour | Apache License, Version 2.0 |
| org.xmlunit:xmlunit-core | test | 2.11.0 | 2.14.0 | mineure/patch en retard | The Apache Software License, Version 2.0 |
| org.yaml:snakeyaml | compile/runtime | 2.6 | 2.7 | mineure/patch en retard | Apache License, Version 2.0 |
| tools.jackson.core:jackson-core | compile/runtime | 3.1.5 | 3.2.3 | mineure/patch en retard | The Apache Software License, Version 2.0 |
| tools.jackson.core:jackson-databind | compile/runtime | 3.1.5 | 3.2.3 | mineure/patch en retard | The Apache Software License, Version 2.0 |
<!-- DEPS:END -->

### Outils de build (hors livrable, non redistribués)

| Plugin | Version | Dernière stable | Licence |
|---|---|---|---|
| `org.springframework.boot:spring-boot-maven-plugin` | 4.1.1 | 4.1.1 | Apache-2.0 |
| `org.jacoco:jacoco-maven-plugin` | 0.8.15 | 0.8.15 | EPL-2.0 |
| `org.codehaus.mojo:license-maven-plugin` | 2.7.1 | 2.7.1 | LGPL-3.0 (voir ci-dessous) |
| `org.cyclonedx:cyclonedx-maven-plugin` | 2.9.3 | 2.9.3 | Apache-2.0 |
| `org.apache.maven.plugins:maven-surefire-plugin` | 3.5.6 | 3.6.0 (mineure en retard, version imposée par le parent Spring Boot 4.1.1) | Apache-2.0 |

### Composants qui posent question

- **`license-maven-plugin` - LGPL-3.0 (copyleft).** Outil de build uniquement : il n'est ni lié à l'application ni redistribué (absent du jar), donc l'obligation LGPL ne s'applique pas. Il est remplaçable sans toucher au code (le scan ne sert qu'à l'audit).
- **`ch.qos.logback:logback-classic` / `logback-core` - EPL-2.0 / LGPL-2.1.** Double licence : on retient la branche **EPL-2.0** (copyleft faible au niveau fichier, aucune modification ni redistribution du code de Logback). Utilisé uniquement via la façade SLF4J : aucun import `ch.qos.logback` dans `src/`, donc remplaçable sans toucher au métier. Exception relue dans `scripts/check_license_whitelist.py`.
- **`jakarta.annotation:jakarta.annotation-api` - EPL-2.0 / GPL-2.0 avec Classpath Exception.** Double licence : branche **EPL-2.0** élue ; la Classpath Exception supprime de toute façon la propagation du copyleft. Simples annotations consommées par le conteneur Spring. Exception relue dans le script.
- **JUnit 5/6 (`Eclipse Public License v2.0`), `jakarta.xml.bind-api` / `jakarta.activation-api` (EDL 1.0, équivalent BSD-3-Clause).** Portée **test** uniquement (via `spring-boot-starter-test`), jamais dans l'artefact livré. EDL 1.0 (BSD-3-Clause) reste dans la liste blanche ; l'EPL n'y est **plus** : chaque artefact EPL (JUnit ×6, logback ×2, `jakarta.annotation-api`) a une exception nominative dans `scripts/check_license_whitelist.py`, de sorte qu'une nouvelle dépendance EPL fait échouer le gate tant qu'elle n'a pas été relue.
- **`com.tngtech.archunit:archunit` - Apache-2.0 / BSD.** Portée test (vérification d'architecture) ; version à jour.
- **Versions « en retard » du tableau.** Toutes sont **imposées par le BOM de Spring Boot 4.1.1**, qui est la dernière version stable de Spring Boot au moment du scan ; nous ne les surchargeons pas pour garder l'ensemble testé ensemble par l'équipe Spring (Logback 1.5.x, Jackson annotations 2.21, JUnit 6.0.x…). Les écarts sont des mineures/patchs, sauf `json-path` (majeure, portée test uniquement). **Exception assumée : Tomcat** est surchargé (`<tomcat.version>11.0.26</tomcat.version>` dans le `pom.xml`, patch de la même mineure que celle du BOM) car les patchs Tomcat embarquent souvent des correctifs de sécurité ; retirer la surcharge dès que le BOM Spring Boot la rattrape. Action : monter Spring Boot dès la prochaine version corrective.

### Services externes consommés (ce ne sont pas des paquets)

- **iTunes Search API** : gratuite, sans clé ; environ 20 requêtes/minute -> cache obligatoire côté application (`CachingMusicProvider`). Elle répond en `Content-Type: text/javascript` : l'adaptateur lit le corps en texte puis le désérialise lui-même. Seuls le titre et l'artiste sont conservés (`trackViewUrl` ne sort pas de l'adaptateur).
- **MusicBrainz** : gratuite, sans clé ; **`User-Agent` identifiable obligatoire** (nom, version, contact) sous peine de refus ; limite d'environ 1 requête/seconde par IP au-delà de laquelle les requêtes sont refusées en `503` (règles de limitation de débit publiées par MusicBrainz) -> cache, et repli automatique vers la liste locale en cas de refus. Vérifier les conditions d'utilisation et la licence des données avant tout usage commercial.
