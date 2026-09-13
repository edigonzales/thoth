# Keycloak for local Biblios development

Starts Keycloak with the `biblios-dev` realm preconfigured (client `biblios`,
test users Anna, Ben and Claudia, groups `agi-betrieb` and `projektteam`).

```bash
docker compose -f dev/keycloak/docker-compose.yml up -d
```

Keycloak admin console: <http://localhost:8090> (admin / local-dev-only).
Realm: `biblios-dev`, issuer: `http://localhost:8090/realms/biblios-dev`.

The client allows the redirect URIs `http://localhost:8080/login/oauth2/code/keycloak`
and `http://localhost:*/login/oauth2/code/keycloak`. If your Biblios dev server
runs on another port (for example because 8080 is taken), either keep the
wildcard entry or add the concrete redirect URI to the client.

| User    | Password | Groups        | Intended access                                   |
|---------|----------|---------------|---------------------------------------------------|
| anna    | anna     | agi-betrieb   | group-based access to the "Betriebshandbuch"      |
| ben     | ben      | –             | authenticated, no group access                    |
| claudia | claudia  | projektteam   | group-based access to another protected doc       |

The group membership mapper emits group names in the `groups` claim of the ID
token. `access.yml` policies reference these names via `provider: keycloak-local`.

These credentials are for local development only and must never be reused.
