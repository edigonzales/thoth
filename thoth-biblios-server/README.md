# Thoth Biblios Server

Authenticated documentation portal for Biblios. It serves a private **publication
package** produced by `thoth-biblios build --package`, enforces per-documentation
access policies and assembles navigation, switchers and search for each visitor.

## 1. Build the publication package

```bash
./gradlew :thoth-biblios:fatJar
java -jar thoth-biblios/build/libs/thoth-biblios-<version>-all.jar build \
  --config biblios.yml \
  --output build/site \
  --package build/package
```

The package contains:

| Path | Purpose |
|------|---------|
| `manifest.json` | authoritative mapping: portal path → documentation (source) |
| `catalog.json` | view models and page metadata for server-side frame rendering |
| `pages/...` | pre-rendered content fragments (no frame, no global navigation) |
| `files/...` | every servable file (theme, images, attachments, PDF/DOCX) |
| `search-index.json` | internal search data (never served directly) |

## 2. Run the server

JVM (development):

```bash
./gradlew :thoth-biblios-server:bootRun --args="--biblios.package-dir=build/package --biblios.access-config=access.yml"
```

Native binary (production; must be built for the target OS/architecture):

```bash
./gradlew :thoth-biblios-server:nativeCompile
BIBLIOS_PACKAGE=build/package BIBLIOS_ACCESS_CONFIG=access.yml \
  thoth-biblios-server/build/native/nativeCompile/thoth-biblios-server
```

Docker: `docker build -f thoth-biblios-server/Dockerfile -t thoth-biblios-server .`

## 3. Configuration

| Property | Environment | Default | Purpose |
|----------|-------------|---------|---------|
| `biblios.package-dir` | `BIBLIOS_PACKAGE` | `build/package` | publication package |
| `biblios.access-config` | `BIBLIOS_ACCESS_CONFIG` | `access.yml` | access rules (re-read on change) |
| `biblios.max-identity-age` | `BIBLIOS_MAX_IDENTITY_AGE` | `60m` | groups are accepted this long after login |
| `biblios.provider` | `BIBLIOS_PROVIDER` | `keycloak-local` | provider name used in policies |
| `biblios.subject-claim` | `BIBLIOS_SUBJECT_CLAIM` | `sub` | stable subject (Entra: `oid`) |
| `biblios.groups-claim` | `BIBLIOS_GROUPS_CLAIM` | `groups` | group memberships |
| `biblios.registration-id` | `BIBLIOS_REGISTRATION_ID` | `keycloak` | Spring Security registration |
| `BIBLIOS_SESSION_TIMEOUT` | | `8h` | servlet session timeout |
| `BIBLIOS_ISSUER_URI` | | `http://localhost:8090/realms/biblios-dev` | OIDC issuer |

The OIDC client is configured in `application.yml`; authorization and token
endpoints are explicit, so the server starts without contacting the provider.

### Access rules (`access.yml`)

```yaml
default: deny
policies:
  public:
    mode: public
  agi-betrieb:
    mode: restricted
    allow:
      groups:
        - provider: entra-kanton
          id: "<group object id>"
      users:
        - provider: entra-kanton
          id: "<user object id>"
```

Documentation sources reference policies via `access_policy` in `biblios.yml`.
Identities are matched by provider + subject (Entra ID: `oid`), never by e-mail.

## 4. Local Keycloak

```bash
docker compose -f dev/keycloak/docker-compose.yml up -d
```

Realm `biblios-dev`, client `biblios` (secret `local-dev-only`), test users
`anna` (group `agi-betrieb`), `ben` (no group) and `claudia` (group
`projektteam`). The dev server default issuer is `http://localhost:8090/realms/biblios-dev`.
See [dev/keycloak/README.md](../dev/keycloak/README.md).

## 5. Entra ID

No Entra tenant is required for development, but the server is prepared for it.
Add a second registration to the server configuration (or an external
`application-entra.yml`) and point the portal at it:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          entra:
            client-id: <application-client-id>
            client-secret: ${ENTRA_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid,profile,email
        provider:
          entra:
            authorization-uri: https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/authorize
            token-uri: https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/token
            jwk-set-uri: https://login.microsoftonline.com/<tenant-id>/discovery/v2.0/keys
            user-info-uri: https://graph.microsoft.com/oidc/userinfo
            user-name-attribute: name

biblios:
  registration-id: entra
  provider: entra-kanton
  subject-claim: oid
  groups-claim: groups
```

App registration (web platform) with redirect URI
`https://<host>/login/oauth2/code/entra`. Policies then reference the tenant's
user and group object IDs (`oid`/group GUID) with `provider: entra-kanton`.

Notes:

- Entra omits group memberships beyond the token size limit ("group overage").
  The server then sees no groups and falls back to direct user grants only
  (fail closed).
- App roles or a Microsoft Graph lookup can be added later if needed.
- No dedicated Entra test tenant is required for the Keycloak-based E2E tests.

## 6. Security behavior

- Every served file must exist in the manifest; unknown or internal paths are 404.
- Protected files, page frames and the search index require the access policy of
  their documentation; anonymous visitors are redirected to the provider and
  return to the requested page after login.
- Protected responses use `Cache-Control: private, no-store` and `Vary: Cookie`;
  shared assets are cacheable for 5 minutes.
- Range requests, HEAD and conditional requests are handled by Spring's resource
  handling after the access check.
- `access.yml` is re-read when it changes (no restart). Invalid updates are
  ignored and the last valid rules stay active.
- Group memberships are accepted for at most `biblios.max-identity-age`; after
  that the session is invalidated and the user logs in again.
- Configuration errors at startup (unknown policy references) abort the server.
- Authenticated visitors get a logout form (POST with CSRF token).

## 7. Tests

```bash
# Unit and access-control tests (MockMvc)
./gradlew :thoth-biblios-server:test

# End-to-end OIDC login against a real Keycloak container (requires Docker)
./gradlew :thoth-biblios-server:e2eTest
```

The E2E test starts Keycloak from `dev/keycloak/realm-biblios-dev.json` in a
container, performs the authorization code flow for `anna` and `ben` and checks
the resulting access decisions. It is skipped automatically when Docker is not
available.

## 8. Known limitations (v1)

- Chapter-level or version-specific rights are not supported; the protection unit
  is a documentation (source) including all versions and files.
- Group memberships reflect the state captured at login and are re-checked at
  most after `biblios.max-identity-age`; there is no background refresh.
- The Entra group overage case is not resolved via Microsoft Graph.
- The publication package is read at startup; rebuild and restart to publish new
  content.
