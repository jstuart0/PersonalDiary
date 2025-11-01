# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with infrastructure in this repository.

## 📚 Reference Documentation

**This CLAUDE.md has been streamlined. Detailed information is in:**

- **[CREDENTIALS.md](docs/reference/CREDENTIALS.md)** - All credential locations, service access, database connections
- **[ARCHITECTURE.md](docs/reference/ARCHITECTURE.md)** - Equipment profiles, network topology, cluster configuration
- **[DEPLOYMENT.md](docs/reference/DEPLOYMENT.md)** - Deployment procedures, operations, backup/recovery
- **[COMMANDS.md](docs/reference/COMMANDS.md)** - Complete kubectl, docker, network command reference

## Repository Overview

This is the **k8s-home-lab** infrastructure repository containing Kubernetes manifests, configurations, and automation for the home laboratory environment. The infrastructure hosts production services (Wiki.js, Authentik, Plane, Rancher, etc.) and provides development/testing environments.

## ⚠️ CRITICAL: Infrastructure vs Application Boundaries

**This repository is ONLY for homelab infrastructure and core services.**

### What Belongs Here

**Core Infrastructure Services:**
- ✅ Authentik SSO/Identity Provider
- ✅ Traefik reverse proxy/ingress controller
- ✅ Rancher Kubernetes management
- ✅ Wiki.js documentation platform
- ✅ Plane project management
- ✅ Cert-manager for TLS certificates
- ✅ Grafana/Prometheus monitoring
- ✅ Webtop virtual desktop
- ✅ Vaultwarden password manager
- ✅ Homarr dashboard
- ✅ Headscale VPN coordination server
- ✅ Headplane web UI for Headscale
- ✅ General cluster services and infrastructure

**Cluster Management:**
- ✅ Kubernetes manifests for infrastructure services
- ✅ Network configuration (MetalLB, ingress rules)
- ✅ Storage management (Ceph, Synology integration)
- ✅ Infrastructure automation scripts
- ✅ Monitoring and observability configurations

### What Does NOT Belong Here

**Application-Specific Work:**
- ❌ Smart Benefit Wallet application (lives in `/Users/jaystuart/dev/Monarch/`)
- ❌ Application-specific databases (unless infrastructure-shared)
- ❌ Application business logic or APIs
- ❌ Mobile app configurations

**Directory Reference:**
- **Homelab/Infrastructure**: `/Users/jaystuart/dev/kubernetes/k8s-home-lab/` 
- **Other Projects**: (define as needed)

## ⚠️ CRITICAL: Always Verify Context Before Commands

**BEFORE running ANY kubectl command, you MUST verify you're targeting the correct cluster and namespace.**

### Why This Is Critical

**One wrong command can:**
- ❌ Delete production infrastructure services
- ❌ Disrupt network connectivity
- ❌ Corrupt Ceph storage
- ❌ Break authentication (Authentik)
- ❌ Take down all services
- ❌ Lose critical configuration

**The homelab cluster is PRODUCTION for infrastructure services. Always verify FIRST.**

### Context Verification Checklist

**BEFORE every kubectl command:**

```bash
# 1. Check current Kubernetes context
kubectl config current-context

# Expected output: "thor" or "kubernetes-admin@kubernetes"
# Context: thor cluster at 192.168.10.222:6443

# 2. If wrong context, STOP and switch
kubectl config use-context thor

# 3. Always verify namespace
kubectl config get-contexts | grep $(kubectl config current-context)

# 4. Best practice: ALWAYS specify namespace explicitly
kubectl -n authentik get pods         # Good - explicit namespace
kubectl -n wiki get pods               # Good - explicit namespace
kubectl get pods                       # Bad - depends on default namespace
```

### Available Namespaces

**Core Infrastructure:**
- `default` - General services, MetalLB, Traefik
- `authentik` - Authentik SSO
- `wiki` - Wiki.js documentation
- `plane` - Plane project management (if deployed here)
- `monitoring` - Grafana, Prometheus
- `automation` - Credential secrets for automation
- `headscale` - Headscale VPN and Headplane UI
- `kube-system` - Kubernetes system components

**Application Namespaces:**
- `smartwallet-local` - Smart Benefit Wallet (development/testing)

**System Namespaces:**
- `cert-manager` - TLS certificate management
- `metallb-system` - MetalLB load balancer

### Command Safety Patterns

```bash
# ✅ GOOD - Explicit context, namespace, and verification
kubectl config current-context  # Verify first
kubectl -n authentik get pods   # Explicit namespace
kubectl -n wiki delete pod wiki-deployment-abc123  # Explicit namespace

# ❌ BAD - Implicit context/namespace
kubectl get pods                # Which context? Which namespace?
kubectl delete pod my-pod       # Dangerous - no namespace specified!
```

## Quick Access Information

### Kubernetes Cluster: thor

**API Server:** https://192.168.10.222:6443
**Context Name:** `thor` or `kubernetes-admin@kubernetes`

**Access:**
```bash
# Switch to thor cluster
kubectl config use-context thor

# Verify connection
kubectl cluster-info
kubectl get nodes
```

### Service URLs

```bash
# Primary Infrastructure Services
https://wiki.xmojo.net          # Documentation
https://auth.xmojo.net          # SSO/Authentication
https://plane.xmojo.net         # Project Management
https://headplane.xmojo.net/admin/  # Headscale VPN Management
https://traefik.xmojo.net       # Traefik Dashboard (if enabled)

# Infrastructure Access
https://192.168.10.222:6443     # Kubernetes API
postgres-01.xmojo.net:5432      # PostgreSQL
192.168.10.159:5000             # Synology DSM
```

### Key Infrastructure IPs

- 192.168.10.1 - UDM Pro (Genesis gateway)
- 192.168.10.11-13 - Ceph cluster nodes
- 192.168.10.14 - Node 4 (backup/database)
- 192.168.10.159 - Synology DS1821+
- 192.168.10.222 - Kubernetes API Server
- 192.168.60.50 - Traefik Load Balancer (MetalLB)

**For complete network topology and equipment details:** See [ARCHITECTURE.md](docs/reference/ARCHITECTURE.md)

## Credentials and Access

**⚠️ CRITICAL: All credentials are stored in Kubernetes secrets or Vaultwarden**

### Quick Credential Retrieval

```bash
# Get Plane API token
PLANE_API_TOKEN=$(kubectl -n automation get secret plane-api-credentials -o jsonpath='{.data.api-token}' | base64 -d)

# Get Wiki.js API key
export WIKIJS_API_KEY=$(kubectl -n automation get secret wikijs-api-credentials -o jsonpath='{.data.api-key}' | base64 -d)
```

**For complete credential information:** See [CREDENTIALS.md](docs/reference/CREDENTIALS.md)

**NEVER commit credentials to git**

## Common Operations

### Deploy New Service

```bash
# Create namespace
kubectl create namespace <service-name>

# Apply manifests (verify context first!)
kubectl config current-context
kubectl apply -f manifests/<service-name>/

# Verify
kubectl -n <service-name> get all
```

### Update Existing Service

```bash
# Verify context
kubectl config current-context

# Update
kubectl apply -f manifests/<service>/<updated-manifest>.yaml

# Watch rollout
kubectl -n <namespace> rollout status deployment/<deployment-name>
```

**For detailed deployment procedures:** See [DEPLOYMENT.md](docs/reference/DEPLOYMENT.md)

## Safety Constraints and Known Issues

### Critical Priorities

**⚠️ CRITICAL: AC Unit Electrical Issue**
- **Status:** Electrical issues preventing AC unit operation
- **Impact:** Cannot increase compute density until resolved
- **Risk:** Equipment thermal throttling and potential damage

**Power Management:**
- Keep idle equipment offline to minimize power draw
- Monitor power consumption and thermal status

### Known High Priority Issues

- ⚠️ **AC Unit Electrical:** Resolve wiring/circuit issues (CRITICAL)
- ⚠️ **USW Aggregation Port 2:** SFP signal loss - clean up or fix
- ⚠️ **Node 2 Hostname:** Shows as "shellyswitch25-68C6" (needs correction)
- ⚠️ **Dev Server Hostname:** Shows as "proxmox-01" (conflicts with cluster naming)

**For complete known issues list:** See [ARCHITECTURE.md](docs/reference/ARCHITECTURE.md)

### Capacity Limits

**Current Resources:**
- Total CPU: 172 cores
- Total RAM: 448GB (Proxmox cluster) + 32GB (Synology)
- Storage: 22TB (Synology) + ~6TB (Ceph, expanding to ~18TB)
- Network: 10GbE core, 120Gbps Ceph backend

**Before adding workloads, verify:**
- Available CPU/RAM capacity
- Storage space (especially Ceph)
- Network bandwidth
- Power availability
- Cooling capacity (AC unit status)

## Task Management and Documentation

### Plane Integration

**Instance:** https://plane.xmojo.net
**Workspace:** agile-solutions-group
**Homelab Project:** Use "smartwallet-infrastructure" module in Smart Benefit Wallet project, or create dedicated homelab project

**When working on infrastructure:**
1. Check Plane for tasks
2. Update progress as work proceeds
3. Move to "Done" when verified
4. Create new tasks for discovered issues

### Wiki Documentation

**Instance:** https://wiki.xmojo.net

**Documentation categories:**
- `homelab/infrastructure/` - Core infrastructure docs
- `homelab/services/` - Service-specific documentation
- `homelab/troubleshooting/` - Common issues and solutions
- `homelab/procedures/` - Standard operating procedures
- `homelab/hardware/` - Equipment documentation

**Best practices:**
- Use UPPERCASE_SNAKE_CASE for infrastructure docs
- Use kebab-case for procedures
- Never commit credentials

**For complete documentation workflow:** See [DEPLOYMENT.md](docs/reference/DEPLOYMENT.md)

## Repository Structure

```
k8s-home-lab/
├── CLAUDE.md                    # This file
├── README.md                    # Repository overview
├── docs/reference/              # Detailed reference documentation
│   ├── CREDENTIALS.md           # Credential locations and access
│   ├── ARCHITECTURE.md          # Equipment and network topology
│   ├── DEPLOYMENT.md            # Deployment procedures
│   └── COMMANDS.md              # Command reference
├── manifests/                   # Kubernetes manifests by service
│   ├── authentik/               # Authentik SSO
│   ├── cert-manager/            # TLS certificate automation
│   ├── grafana/                 # Grafana monitoring
│   ├── headscale/               # Headscale VPN and Headplane UI
│   ├── homarr/                  # Dashboard
│   ├── plane/                   # Project management
│   ├── prometheus/              # Metrics collection
│   ├── rancher/                 # Kubernetes management
│   ├── traefik/                 # Reverse proxy
│   ├── vaultwarden/             # Password manager
│   ├── webtop/                  # Virtual desktop
│   └── wiki/                    # Wiki.js documentation
├── scripts/                     # Automation scripts
└── secrets/                     # Secret management templates
```

## Technology Stack

**Infrastructure:**
- Proxmox VE (virtualization)
- Kubernetes (container orchestration)
- Ceph (distributed storage)
- Traefik (ingress/reverse proxy)
- MetalLB (load balancer)
- Cert-manager (TLS certificates)

**Services:**
- Authentik (SSO/identity)
- Wiki.js (documentation)
- Plane (project management)
- Rancher (Kubernetes management)
- Grafana/Prometheus (monitoring)
- Headscale/Headplane (VPN)

**Hardware:**
- Minisforum MS-01/MS-A2 (compute nodes)
- NVIDIA Jetson Orin Nano (edge AI)
- UniFi networking equipment
- Synology DS1821+ (NAS)

**For complete architecture details:** See [ARCHITECTURE.md](docs/reference/ARCHITECTURE.md)

## Quick Command Reference

```bash
# Context verification
kubectl config current-context
kubectl config use-context thor

# Pod operations
kubectl get pods -A
kubectl -n <namespace> logs -f <pod-name>
kubectl -n <namespace> describe pod <pod-name>

# Deployment operations
kubectl -n <namespace> rollout status deployment/<name>
kubectl -n <namespace> rollout restart deployment/<name>

# Secret operations
kubectl -n <namespace> get secrets
kubectl get secret <secret-name> -n <namespace> -o jsonpath='{.data.<key>}' | base64 -d
```

**For complete command reference:** See [COMMANDS.md](docs/reference/COMMANDS.md)

## Related Resources

- Kubernetes Documentation: https://kubernetes.io/docs/
- Traefik Documentation: https://doc.traefik.io/traefik/
- Authentik Documentation: https://docs.goauthentik.io/
- Wiki.js Documentation: https://docs.requarks.io/
- Ceph Documentation: https://docs.ceph.com/
- Proxmox Documentation: https://pve.proxmox.com/pve-docs/
- UniFi Documentation: https://help.ui.com/

---

**Last Updated:** October 30, 2025
**Maintained By:** Jay Stuart
**Repository:** `/Users/jaystuart/dev/kubernetes/k8s-home-lab/`
