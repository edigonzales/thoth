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

JVM as a plain Java application (recommended for local development; needs Java 25):

```bash
./gradlew :thoth-biblios-server:bootJar
~/.sdkman/candidates/java/25.0.3-tem/bin/java \
  -jar thoth-biblios-server/build/libs/thoth-biblios-server-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=dev \
  --biblios.package-dir=build/package \
  --biblios.access-config=access.yml
```

The `dev` profile only sets the port to **8091** (8080/8081 are often occupied
by other local stacks). Without a profile the server uses port 8080. See
[dev/keycloak/README.md](../dev/keycloak/README.md) for the full Keycloak +
Java application walkthrough.

Alternative via Gradle: `./gradlew :thoth-biblios-server:bootRun
--args="--spring.profiles.active=dev --biblios.package-dir=... --biblios.access-config=..."`.

Native binary (production; must be built for the target OS/architecture).
Requires a GraalVM 25 JDK: either start Gradle with `JAVA_HOME` pointing to it
or set `GRAALVM_HOME`.

```bash
GRAALVM_HOME=/path/to/graalvm-25 ./gradlew :thoth-biblios-server:nativeCompile
BIBLIOS_PORT=8091 BIBLIOS_PACKAGE=build/package BIBLIOS_ACCESS_CONFIG=access.yml \
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
| `biblios.issuer-uri` | `BIBLIOS_ISSUER_URI` | `http://localhost:8090/realms/biblios-dev` | exact expected ID-token issuer |

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
The configured subject claim must be a nonempty string; a missing `oid`, for example,
does not fall back to `sub`.

The server requires a readable, valid `access.yml` at startup. An explicitly public
installation without named policies can use:

```yaml
default: public
```

If the file disappears or becomes unreadable at runtime, all documentation is
denied, including previously public pages and attachments. Navigation and search
contain no documentation; shared theme assets remain available. Direct requests
receive the same generic 404 as unknown targets, for all visitors. Restoring a fully
valid file restores the configured permissions automatically, even when its
modification timestamp is unchanged. The server logs entry into and recovery from
this deny-all state.

Readable malformed updates also deny all documentation immediately, including
when a policy still referenced by the package is removed. There is no grace period
or fallback to previous grants. Only a fully valid file restores access.

The complete UTF-8 content is read on each access decision and compared with the
last checked content. Changes apply even with identical timestamps and file sizes;
unchanged invalid content is not repeatedly parsed or logged. Publish rule updates
atomically (write a new file, then rename it) to avoid temporary invalid states.
Revocation applies to the next access decision; transfers already underway are not
retroactively interrupted.

### Expected OIDC issuer

`biblios.issuer-uri` is required and must exactly match the ID token's `iss`,
including any trailing slash. It must be an absolute HTTP(S) URI with a host and
without user information, query or fragment. The value is not normalized.
`BIBLIOS_ISSUER_URI` continues to configure both the default Keycloak endpoints and
the expected issuer. Other providers must explicitly set `biblios.issuer-uri`.

The decoder adds this check to Spring's existing signature, audience, time and
OIDC validation. A selected registration that already declares an issuer must
agree with this value or startup fails. Explicit endpoints remain supported;
setting `biblios.issuer-uri` does not trigger discovery or a JWK fetch at startup.

Existing sessions with a missing or different ID-token issuer no longer grant
documentation access and require a new login. There is no switch to disable this
validation and no multi-tenant issuer allowlist.

### Hidden documentation and explicit login

A documentation 404 means "not present or not visible to this visitor". Components,
versions, pages and files use the same generic response for unknown and unauthorized
targets, whether or not the visitor is signed in. Redirects to a component's slash
URL or a version's start page happen only after authorization. HEAD, Range and
conditional requests cannot expose file metadata before that check.

Every documentation 404 has a **Sign in** link, including unknown targets. Choosing
it starts `/login?returnTo=...`; the validated local path and query are saved for one
successful login. The target is then requested again and its permissions rechecked;
an unknown target or an unauthorized user still receives 404. No target is saved
merely by requesting a missing/hidden page. General portal login links return to `/`.
External URLs and login/logout/OAuth2 return targets are rejected in favor of the
portal start page. Logout continues to return to the portal without starting a login.

This intentionally changes unauthorized documentation responses from 302/403 to 404.
HTTP clients that previously relied on automatic login redirects or a 403 distinction
must adapt. Existing public knowledge of document names cannot be retracted, and
this does not promise indistinguishable response times. Package and policy schemas
are unchanged; there is no switch to restore the old distinguishable responses.

### Upgrade requirements

- Supply an explicit `access.yml`, including for entirely public portals.
- Select an existing client with `biblios.registration-id`. Only this registration
  may start or complete a login; other configured registrations return 404, and
  their existing sessions grant no documentation access. `/login` redirects to the
  selected provider. `biblios.provider` names that selected provider in policies.
- Source IDs must be single, nonempty path segments, excluding `.`, `..`, `/`, and
  `\`. The ID `site-assets` is reserved, regardless of letter case. Rename any
  such documentation and rebuild its package before upgrading; its URLs change.
  Existing packages with the reserved ID or invalid shared mappings are rejected.
- The publication package format is unchanged. Shared entries must be beneath
  `site-assets/` and must not carry a source assignment. Static builds retain their
  existing behavior when no access file is present.

## 4. Local Keycloak

Keycloak runs as a container on port **8090**, the server as a Java application
on port **8091** (both free of typical local stacks).

```bash
docker compose -f dev/keycloak/docker-compose.yml up -d

# Test package with a public and a protected documentation
./gradlew :thoth-biblios-server:generateSmokePackage
./gradlew :thoth-biblios-server:bootJar

~/.sdkman/candidates/java/25.0.3-tem/bin/java \
  -jar thoth-biblios-server/build/libs/thoth-biblios-server-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=dev \
  --biblios.package-dir=thoth-biblios-server/build/smoke-package/package \
  --biblios.access-config=thoth-biblios-server/build/smoke-package/access.yml
```

Realm `biblios-dev`, client `biblios` (secret `local-dev-only`), test users
`anna` (group `agi-betrieb`), `ben` (no group) and `claudia` (group
`projektteam`). The issuer is `http://localhost:8090/realms/biblios-dev` and the
client only accepts the concrete callback URLs, not port wildcards.

The complete walkthrough (ports, users, redirect URIs, hot-reload and
identity-age experiments) is in
[dev/keycloak/README.md](../dev/keycloak/README.md).

## 5. Entra ID

No Entra tenant is required for development, but the server is prepared for it.
Add an Entra registration to the server configuration (or an external
`application-entra.yml`) and select it as the portal's sole permitted login.
The existing Keycloak registration can remain configured, but its login endpoints
and existing sessions will no longer grant access:

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
  issuer-uri: https://login.microsoftonline.com/<tenant-id>/v2.0
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
  URI paths are decoded once, preserving `+`. The resource authorized from the
  manifest is the exact resource served, including for HEAD and Range requests.
- Protected files, page frames and the search index require the access policy of
  their documentation. Unknown and unauthorized documentation targets both return
  404; visitors explicitly choose the login link to sign in and return to the target.
- Protected responses use `Cache-Control: private, no-store` and `Vary: Cookie`;
  shared assets are cacheable for 5 minutes.
- Range requests, HEAD and conditional requests are handled by Spring's resource
  handling after the access check.
- `access.yml` is mandatory. Missing, unreadable or invalid rules deny all
  documentation until a fully valid file returns. Changes are detected from file
  contents, independently of timestamp and size.
- ID tokens and existing sessions must match `biblios.issuer-uri` exactly; standard
  signature, time and audience validation remains active.
- Group memberships are accepted for at most `biblios.max-identity-age`; after
  that the session is invalidated and the user logs in again.
- Configuration errors at startup (unknown policy references) abort the server.
- Authenticated visitors get a logout form (POST with CSRF token), returning to
  the public portal without starting another OIDC login.

## 7. Tests

```bash
# Unit and access-control tests (MockMvc)
./gradlew :thoth-biblios-server:test

# End-to-end OIDC login against a real Keycloak container (requires Docker)
./gradlew :thoth-biblios-server:e2eTest
```

The E2E test starts Keycloak from `dev/keycloak/import/realm-biblios-dev.json` in a
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
