# Credentials and Access

**⚠️ CRITICAL: Credentials are stored in TWO locations:**

1. **Kubernetes Secrets** - Primary location for automation
2. **Vaultwarden** - Backup and manual access

## Credential Locations

### Kubernetes Secrets (Primary)

**All automation credentials are in the `automation` namespace on thor cluster:**

```bash
# Switch to thor cluster
kubectl config use-context thor

# View available credential secrets
kubectl -n automation get secrets

# Retrieve a credential (example: Plane API token)
kubectl -n automation get secret plane-api-credentials -o jsonpath='{.data.api-token}' | base64 -d
```

**Available Secrets in `automation` Namespace:**

- `plane-api-credentials` - Plane Project Management API
  - api-token, instance-url, workspace-slug, project-id

- `wikijs-api-credentials` - Wiki.js Documentation API
  - api-key, instance-url

- `sqlserver-credentials` - SQL Server Database
  - app-user, app-password, admin-user-aws, admin-password-aws
  - admin-user-local, admin-password-local
  - connection-string-aws, connection-string-local

- `hapi-fhir-postgres-credentials` - HAPI FHIR PostgreSQL
  - host, database, user, password, jdbc-url

- `keycloak-admin-credentials` - Keycloak Admin
  - admin-username, admin-password
  - db-host, db-port, db-name, db-user, db-password

**Retrieve Credentials:**

```bash
# Get Plane API token
PLANE_API_TOKEN=$(kubectl -n automation get secret plane-api-credentials -o jsonpath='{.data.api-token}' | base64 -d)

# Get Wiki.js API key
export WIKIJS_API_KEY=$(kubectl -n automation get secret wikijs-api-credentials -o jsonpath='{.data.api-key}' | base64 -d)

# Get SQL Server connection string (AWS)
SQL_CONN=$(kubectl -n automation get secret sqlserver-credentials -o jsonpath='{.data.connection-string-aws}' | base64 -d)
```

**Infrastructure Service Secrets:**

```bash
# Authentik secrets
kubectl -n authentik get secrets

# Wiki.js secrets
kubectl -n wiki get secrets

# Cert-manager secrets (TLS certificates)
kubectl get secret wildcard-xmojo-net-production-tls -o yaml
```

### Vaultwarden (Backup / Manual Access)

**When Kubernetes secrets are unavailable or for manual access:**

1. Access Vaultwarden at: (document URL when confirmed)
2. Search for credential by service name
3. Use for manual operations or emergency recovery

**Best Practices:**
- ✅ Use Kubernetes secrets for automation
- ✅ Keep Vaultwarden synchronized with Kubernetes secrets
- ✅ Document which credentials exist in both locations
- ❌ Never commit credentials to git
- ❌ Never store credentials in plaintext files

## PostgreSQL Databases

**Primary Database Server: postgres-01.xmojo.net:5432**

**Databases:**
- `wikijs` - Wiki.js documentation
- `authentik` - Authentik SSO
- `smartbenefit` - Smart Benefit Wallet (application)
- `keycloak` - Keycloak OAuth2 (if deployed here)

**Admin User:** `psadmin`
**Password:** Check `automation` namespace secrets or Vaultwarden

**Access:**
```bash
# Connect to Wiki.js database
psql -h postgres-01.xmojo.net -U psadmin -d wikijs

# Connect to Authentik database
psql -h postgres-01.xmojo.net -U psadmin -d authentik
```

## Service-Specific Access

### Authentik Admin Console

- URL: https://auth.xmojo.net/if/admin
- Recovery Token: Stored in Vaultwarden (never commit to git)
- Purpose: Configure SSO applications and users

### Wiki.js Admin

- URL: https://wiki.xmojo.net/a
- API Key: Generate from Admin UI or retrieve from secrets
- Purpose: Documentation management

### Plane Admin

- URL: https://plane.xmojo.net
- Workspace: agile-solutions-group
- API Config: Check `automation/plane-api-credentials` secret

### Cloudflare DNS

- Dashboard: https://dash.cloudflare.com
- Zone: xmojo.net
- API Token: Stored in Vaultwarden
- DNS Records: wiki.xmojo.net, auth.xmojo.net, plane.xmojo.net → 192.168.60.50

## Quick Reference: Finding Credentials

**When you need a credential:**

1. **Check thor cluster secrets FIRST:**
   ```bash
   kubectl config use-context thor
   kubectl -n automation get secrets -o name
   kubectl get secret <secret-name> -n <namespace> -o jsonpath='{.data.<key>}' | base64 -d
   ```

2. **Check service-specific namespaces:**
   ```bash
   kubectl -n authentik get secrets
   kubectl -n wiki get secrets
   ```

3. **Check Vaultwarden as backup:**
   - Search by service name
   - Use for manual operations

4. **Document new credentials:**
   - Add to appropriate Kubernetes secret
   - Back up in Vaultwarden
   - Update this documentation if needed

## Service URLs

```bash
# Primary Infrastructure Services
https://wiki.xmojo.net          # Documentation
https://auth.xmojo.net          # SSO/Authentication
https://plane.xmojo.net         # Project Management
https://traefik.xmojo.net       # Traefik Dashboard (if enabled)

# Infrastructure Access
https://192.168.10.222:6443     # Kubernetes API
postgres-01.xmojo.net:5432      # PostgreSQL
192.168.10.159:5000             # Synology DSM
```
