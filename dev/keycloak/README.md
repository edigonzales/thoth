# Keycloak for local development

Keycloak runs as a container, the Biblios server as a plain Java application.
The ports are chosen so that they do not collide with common local stacks
(Jenkins/Caddy and others often occupy 8080/8081).

| Service | Port | URL |
|---------|------|-----|
| Keycloak | **8090** | <http://localhost:8090> |
| Biblios server (dev profile) | **8091** | <http://localhost:8091> |

Commands below assume the repository root as the working directory. Replace
`<version>` in JAR filenames with the version produced by your Gradle build.

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
`BIBLIOS_ISSUER_URI` configures the default endpoints and `biblios.issuer-uri`,
which is compared exactly with the ID token's issuer. For another provider,
explicitly set `biblios.issuer-uri` to its expected issuer (including the tenant
and any trailing slash). It must be an absolute HTTP(S) URI with a host and no
user information, query or fragment. No discovery call is added at startup.
Existing sessions with missing or different issuers grant no access and need a
new login.

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
java \
  -jar thoth-biblios-server/build/libs/thoth-biblios-server-<version>.jar \
  --spring.profiles.active=dev \
  --biblios.package-dir=thoth-biblios-server/build/smoke-package/package \
  --biblios.access-config=thoth-biblios-server/build/smoke-package/access.yml
```

Open <http://localhost:8091>:

- Anonymous: only **Public Docs** is visible; **Internal Docs** is hidden in the
  navigation, the search index and the home page.
- Opening a protected link returns a generic 404, just like an unknown URL.
  Choose **Sign in** on that page to log in with Keycloak and return to the same
  local path and query. An unknown target still returns 404 after login.
- `anna` / `anna` (group `agi-betrieb`) can read the protected documentation.
- `ben` / `ben` gets the same 404 and never sees protected titles.
- The portal also offers a general **Sign in** link, returning to the start page.
  HTTP clients must explicitly start login; protected targets no longer send
  automatic login redirects or distinguishable 403 responses.

The `dev` profile only sets the port (8091). The package and access file are
passed explicitly, so you can point them at your own build output instead.
The access file must exist and be valid before startup, even for public portals
(use `default: public` for sources without named policies).

`biblios.registration-id` selects the only permitted login registration (default:
`keycloak`). Extra configured registrations cannot start or complete logins, and
sessions created through them grant no documentation access. Keep
`biblios.provider=keycloak-local` and `biblios.subject-claim=sub` for this realm.
A configured subject claim must be present; the server does not fall back to
another claim.

Source IDs must be single nonempty path segments; `site-assets` is reserved in all
letter cases. Rename any existing source with that ID and rebuild its package
before upgrading; its documentation URLs change.

Alternative without building the jar: `./gradlew :thoth-biblios-server:bootRun
--args="--spring.profiles.active=dev --biblios.package-dir=... --biblios.access-config=..."`.

## Exercising changes

- **Access rules without restart:** edit
  `thoth-biblios-server/build/smoke-package/access.yml` (for example set the
  `internal` policy to `mode: public` and remove its `allow` block, which is only
  valid for restricted policies). The next access decision reads and compares
  the file contents, including when timestamp and size are unchanged. Invalid
  updates (including removal of a referenced policy) immediately deny all
  documentation until fully valid rules return. Unchanged invalid contents do not
  produce repeated diagnostics. Publish updates atomically by writing a new file
  and renaming it; there is no fallback to old grants. Already running transfers
  are not retroactively interrupted.
- **Missing access rules:** temporarily move `access.yml` away. All documentation,
  including public pages and attachments, is now denied; the search and navigation
  contain no documentation. Theme assets remain accessible. Move a fully valid
  file back: the next access check restores permissions even with the original
  timestamp. Invalid restoration attempts keep the deny-all state. The server logs
  the loss and recovery. An unreadable file has the same behavior.
- **Identity age:** start with `--biblios.max-identity-age=1m`, log in and wait
  one minute; the next protected request returns the generic 404 with a login link.
  Choosing that link starts a fresh login.
- **Native binary:** see
  [thoth-biblios-server/README.md](../../thoth-biblios-server/README.md) (requires a
  GraalVM 25 JDK, `GRAALVM_HOME=... ./gradlew :thoth-biblios-server:nativeCompile`).
