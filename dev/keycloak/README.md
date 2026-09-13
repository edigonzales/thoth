# Keycloak for local development

Keycloak runs as a container, the Biblios server as a plain Java application.
The ports are chosen so that they do not collide with common local stacks
(Jenkins/Caddy and others often occupy 8080/8081).

| Service | Port | URL |
|---------|------|-----|
| Keycloak | **8090** | <http://localhost:8090> |
| Biblios server (dev profile) | **8091** | <http://localhost:8091> |

## Start and stop Keycloak

```bash
# Start
docker compose -f dev/keycloak/docker-compose.yml up -d

# Stop (container and database stay)
docker compose -f dev/keycloak/docker-compose.yml stop

# Remove (realm is imported freshly on the next start)
docker compose -f dev/keycloak/docker-compose.yml down
```

Admin console: <http://localhost:8090> (`admin` / `local-dev-only`).
Issuer: `http://localhost:8090/realms/biblios-dev`.

## Redirect URIs

The client `biblios` accepts these exact callbacks:

- `http://localhost:8091/login/oauth2/code/keycloak`
- `http://127.0.0.1:8091/login/oauth2/code/keycloak`
- `http://localhost:8080/login/oauth2/code/keycloak`
- `http://127.0.0.1:8080/login/oauth2/code/keycloak`

Keycloak does **not** match a port wildcard such as `http://localhost:*`. If you
run the server on another port, either add the concrete callback in the admin
console (lost when the container is removed) or add it to
`import/realm-biblios-dev.json` and recreate the container.

## Test users

| User | Password | Groups | Intended access |
|------|----------|--------|-----------------|
| anna | anna | `agi-betrieb` | group-based access to the protected documentation |
| ben | ben | – | authenticated, no group access |
| claudia | claudia | `projektteam` | group-based access to another protected doc |

The group membership mapper emits group names in the `groups` claim of the ID
token. `access.yml` policies reference them with `provider: keycloak-local`.
These credentials are for local development only.

## Full local walkthrough (Keycloak container + Java application)

```bash
# 1) Keycloak
docker compose -f dev/keycloak/docker-compose.yml up -d

# 2) Create a package with a public and a protected documentation
#    (generated from test code, no Git repositories needed)
./gradlew :thoth-biblios-server:generateSmokePackage

# 3) Build the Spring Boot jar
./gradlew :thoth-biblios-server:bootJar

# 4) Run the server as a Java application (needs Java 25)
~/.sdkman/candidates/java/25.0.3-tem/bin/java \
  -jar thoth-biblios-server/build/libs/thoth-biblios-server-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=dev \
  --biblios.package-dir=thoth-biblios-server/build/smoke-package/package \
  --biblios.access-config=thoth-biblios-server/build/smoke-package/access.yml
```

Open <http://localhost:8091>:

- Anonymous: only **Public Docs** is visible; **Internal Docs** is hidden in the
  navigation, the search index and the home page.
- Clicking a protected link redirects to Keycloak and returns to the requested
  page after login.
- `anna` / `anna` (group `agi-betrieb`) can read the protected documentation.
- `ben` / `ben` gets 403 and never sees protected titles.

The `dev` profile only sets the port (8091). The package and access file are
passed explicitly, so you can point them at your own build output instead.

Alternative without building the jar: `./gradlew :thoth-biblios-server:bootRun
--args="--spring.profiles.active=dev --biblios.package-dir=... --biblios.access-config=..."`.

## Exercising changes

- **Access rules without restart:** edit
  `thoth-biblios-server/build/smoke-package/access.yml` (for example set the
  `internal` policy to `mode: public`). The next request picks it up; invalid
  updates are ignored and the last valid rules stay active.
- **Identity age:** start with `--biblios.max-identity-age=1m`, log in and wait
  one minute; the next request asks for a fresh login.
- **Native binary:** see
  [thoth-biblios-server/README.md](../thoth-biblios-server/README.md) (requires a
  GraalVM 25 JDK, `GRAALVM_HOME=... ./gradlew :thoth-biblios-server:nativeCompile`).
