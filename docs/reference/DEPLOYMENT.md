# Deployment and Operations Guide

## Deploying a New Service

**Standard workflow:**

1. **Create namespace (if needed):**
   ```bash
   kubectl create namespace <service-name>
   ```

2. **Create secrets (if needed):**
   ```bash
   kubectl -n <service-name> create secret generic <secret-name> \
     --from-literal=key1=value1 \
     --from-literal=key2=value2
   ```

3. **Apply manifests:**
   ```bash
   # Verify context first
   kubectl config current-context

   # Apply configuration
   kubectl apply -f manifests/<service-name>/

   # Verify deployment
   kubectl -n <service-name> get all
   kubectl -n <service-name> get pods -w
   ```

4. **Configure ingress (Traefik):**
   ```yaml
   apiVersion: networking.k8s.io/v1
   kind: Ingress
   metadata:
     name: <service-name>-ingress
     namespace: <service-name>
     annotations:
       cert-manager.io/cluster-issuer: letsencrypt-production
   spec:
     ingressClassName: traefik
     tls:
     - hosts:
       - <service>.xmojo.net
       secretName: <service>-tls
     rules:
     - host: <service>.xmojo.net
       http:
         paths:
         - path: /
           pathType: Prefix
           backend:
             service:
               name: <service>
               port:
                 number: <port>
   ```

5. **Configure DNS (Cloudflare):**
   - Add A record: `<service>.xmojo.net` → `192.168.60.50`
   - DNS-only (not proxied)

6. **Verify service:**
   ```bash
   curl https://<service>.xmojo.net
   ```

7. **Document in Wiki:**
   - Service purpose and configuration
   - Deployment instructions
   - Troubleshooting notes

8. **Track in Plane:**
   - Create completion ticket
   - Link to Wiki documentation

## Updating an Existing Service

```bash
# Verify context
kubectl config current-context

# Update manifest
kubectl apply -f manifests/<service>/<updated-manifest>.yaml

# Watch rollout
kubectl -n <namespace> rollout status deployment/<deployment-name>

# View logs
kubectl -n <namespace> logs -f deployment/<deployment-name>

# Rollback if needed
kubectl -n <namespace> rollout undo deployment/<deployment-name>
```

## Managing Storage (Ceph)

**⚠️ CRITICAL: Ceph operations affect all cluster nodes**

### View Ceph Status

```bash
# SSH to any Ceph node (Node 1, 2, or 3)
ssh root@192.168.10.11  # Node 1
ssh root@192.168.10.12  # Node 2
ssh root@192.168.10.13  # Node 3

# Check cluster health
ceph -s
ceph health detail

# View OSDs
ceph osd tree
ceph osd status

# View pools
ceph osd pool ls detail
```

### Managing Kubernetes PVs with Ceph

```bash
# View persistent volumes
kubectl get pv
kubectl get pvc -A

# Create new PVC using Ceph
kubectl apply -f manifests/<service>/<pvc-manifest>.yaml
```

**⚠️ Before expanding Ceph:**
- Ensure AC unit is operational (thermal management)
- Verify power availability
- Plan for NVMe installation on all 3 nodes
- Document current capacity before changes

## Network Configuration

### MetalLB IP Pool

```bash
# View current IP pool configuration
kubectl -n metallb-system get ipaddresspool

# Update IP pool (if needed)
kubectl -n metallb-system edit ipaddresspool
```

### Traefik Configuration

```bash
# View Traefik service
kubectl get svc -A | grep traefik

# Check Traefik ingress routes
kubectl get ingressroute -A

# View Traefik middleware
kubectl get middleware -A
```

### Firewall (UniFi UDM Pro)

- Access via https://192.168.10.1
- Create rules for new services if needed
- Document any port forwards or NAT rules

## Monitoring and Troubleshooting

### View Cluster Resources

```bash
# Node status
kubectl get nodes -o wide
kubectl top nodes

# Pod status across all namespaces
kubectl get pods -A
kubectl top pods -A

# Service status
kubectl get svc -A
kubectl get ingress -A
```

### View Logs

```bash
# Service logs
kubectl -n <namespace> logs -f deployment/<deployment-name>

# Previous logs (if pod crashed)
kubectl -n <namespace> logs --previous <pod-name>

# Tail logs for multiple pods
kubectl -n <namespace> logs -f -l app=<app-label>
```

### Common Troubleshooting

```bash
# Describe resource for events
kubectl -n <namespace> describe pod <pod-name>
kubectl -n <namespace> describe deployment <deployment-name>

# Check persistent volume claims
kubectl get pvc -A
kubectl describe pvc <pvc-name> -n <namespace>

# Check ingress configuration
kubectl describe ingress <ingress-name> -n <namespace>

# Test connectivity
kubectl run test-pod --rm -it --image=busybox -- sh
# Inside pod: wget http://<service>.<namespace>.svc.cluster.local
```

## Backup and Recovery

### Proxmox Backup Server (Node 4)

- Access: (document URL when configured)
- Purpose: VM/Container backups for Proxmox nodes
- Schedule: (document backup schedules)

### Synology Backup Target

- Mount path: (document NFS/SMB mounts)
- Purpose: Long-term backup storage
- Capacity: 22TB available

### Kubernetes Backup

```bash
# Export all resources (for disaster recovery)
kubectl get all -A -o yaml > cluster-backup-$(date +%Y%m%d).yaml

# Backup specific namespace
kubectl get all -n <namespace> -o yaml > <namespace>-backup-$(date +%Y%m%d).yaml

# Backup secrets (CAREFUL - contains sensitive data)
kubectl get secrets -A -o yaml > secrets-backup-$(date +%Y%m%d).yaml
# Store securely, never commit to git
```

### Database Backups

```bash
# PostgreSQL backup
pg_dump -h postgres-01.xmojo.net -U psadmin -d wikijs > wikijs-backup-$(date +%Y%m%d).sql
pg_dump -h postgres-01.xmojo.net -U psadmin -d authentik > authentik-backup-$(date +%Y%m%d).sql
```

## Task Management and Documentation

### Plane Integration

**Instance:** https://plane.xmojo.net
**Workspace:** agile-solutions-group
**Homelab Project:** Use "smartwallet-infrastructure" module in Smart Benefit Wallet project, or create dedicated homelab project

**When working on infrastructure:**

1. **Check Plane for tasks:**
   - Review homelab project backlog
   - Identify assigned work items
   - Note dependencies and priorities

2. **Update progress:**
   - Move tasks to "In Progress" when starting
   - Add comments with technical details
   - Update completion percentage

3. **Complete tasks:**
   - Move to "Done" when verified
   - Add verification steps in comments
   - Link to Wiki documentation

4. **Create new tasks:**
   - Discovered issues or improvements
   - Technical debt identified
   - Required maintenance work

### Wiki Documentation

**Instance:** https://wiki.xmojo.net

**Documentation categories:**

- `homelab/infrastructure/` - Core infrastructure docs
- `homelab/services/` - Service-specific documentation
- `homelab/troubleshooting/` - Common issues and solutions
- `homelab/procedures/` - Standard operating procedures
- `homelab/hardware/` - Equipment documentation

**When creating documentation:**

1. Create markdown file in appropriate category
2. Add to `.wiki-docs-registry.json` (if using import automation)
3. Include:
   - Purpose and overview
   - Configuration details
   - Access credentials location (never the actual credentials)
   - Troubleshooting steps
   - Related services

4. Import to Wiki:
   ```bash
   # If import automation is set up
   python3 /tmp/import-all-docs.py
   ```

**Best practices:**
- Use UPPERCASE_SNAKE_CASE for infrastructure docs (e.g., `AUTHENTIK_SETUP.md`)
- Use kebab-case for procedures (e.g., `service-deployment-procedure.md`)
- Include examples and commands
- Link to related documentation
- Never commit credentials

### High-Level Decision Making

**For architectural decisions or major changes:**

1. **Create Wiki page** in `homelab/decisions/`
   - Problem statement
   - Options considered
   - Pros and cons of each
   - Recommendation
   - Implementation plan

2. **Create Plane epic** for large initiatives
   - Break down into smaller tasks
   - Assign priorities
   - Track dependencies

3. **Review with team** (if applicable)
   - Gather feedback
   - Update decision document
   - Finalize approach

4. **Document outcome:**
   - Update Wiki with final decision
   - Close related Plane tasks
   - Create follow-up tasks if needed
