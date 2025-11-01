# Home Lab Architecture

## Network Infrastructure

### Core Gateway

**UniFi Dream Machine Pro (Genesis)** - 192.168.10.1
- 10G SFP+ WAN + 8-port GbE switch
- IDS/IPS capable, manages 100+ devices

### Distribution Layer

**UniFi Switch Aggregation** - 192.168.10.182
- 8x 10GbE SFP+ ports
- Node 1-3 connected (Ports 1, 5, 6)
- Uplinks: Port 7 to USW Pro HD 24 PoE, Port 8 to UDM Pro

**UniFi Switch Pro 24 HD PoE** - 192.168.10.99
- 24x GbE PoE++ + 2x 10GbE uplinks
- Node 4 on Port 27 (10GbE)
- Dev Server on Port 21 (1GbE)
- 6 Access Points powered via PoE

### Wireless

- 6 Access Points deployed (200+ clients total)

## Compute Cluster - thor (Proxmox)

### Ceph Cluster Nodes (3 nodes)

**Node 1 - MS-01 (192.168.10.11)**
- CPU: Intel i9-13900H (14 cores, 20 threads)
- RAM: 96GB DDR5-5200
- Storage: 2TB NVMe (expanding to +4TB)
- Network: 10GbE SFP+ (frontend) + Thunderbolt 4 40Gbps (Ceph backend)
- Role: Ceph OSD, compute workloads

**Node 2 - MS-01 (192.168.10.12)**
- CPU: Intel i9-13900H (14 cores, 20 threads)
- RAM: 96GB DDR5-5200
- Storage: 2TB NVMe (expanding to +4TB)
- Network: 10GbE SFP+ (frontend) + Thunderbolt 4 40Gbps (Ceph backend)
- ⚠️ Known Issue: Hostname shows as "shellyswitch25-68C6" (needs correction)
- Role: Ceph OSD, compute workloads

**Node 3 - MS-01 (192.168.10.13)**
- CPU: Intel i9-12900H (14 cores, 20 threads)
- RAM: 96GB DDR5-5200
- Storage: 2TB NVMe (expanding to +4TB)
- Network: 10GbE SFP+ (frontend) + Thunderbolt 4 40Gbps (Ceph backend)
- Role: Ceph OSD, compute workloads

**Ceph Backend Network:**
- Token-ring topology via Thunderbolt 4 (40Gbps per link)
- Dedicated backend traffic, isolated from frontend
- Node 1 ↔ Node 2 ↔ Node 3 ↔ Node 1 (ring)

### Backup & Database Node

**Node 4 - MS-A2 (192.168.10.14)**
- CPU: AMD Ryzen 9 9955HX (16 cores, 32 threads)
- RAM: 96GB DDR5-5600
- Storage: 3x M.2 NVMe PCIe 4.0
- Network: 10GbE SFP+ (no Thunderbolt)
- Role: Proxmox Backup Server, PostgreSQL, SQL Server

### Development Node

**Dev Server - KAMRUI Mini PC (192.168.10.10)**
- CPU: Intel i5-12450H (8 cores, 12 threads)
- RAM: 64GB DDR4
- Network: 1GbE only
- Role: Kubernetes runner for development/staging
- ⚠️ Hostname collision: Currently "proxmox-01" (needs rename)

## Storage Infrastructure

### Synology DiskStation DS1821+ (192.168.10.159)

- CPU: AMD Ryzen V1500B (4-core @ 2.2GHz)
- RAM: 32GB DDR4 ECC
- Storage: 22TB (7x SSDs: 3x 2TB + 4x 4TB)
- File System: Btrfs
- Cache: 2x 512GB NVMe M.2
- Network: Dual 10GbE (LACP capable)
- Role: Centralized storage, backup target, media

### Ceph Distributed Storage

- Current: ~6TB across 3 nodes
- Planned: +12TB expansion (4TB per node)
- Backend: 120Gbps aggregate (3x 40Gbps Thunderbolt ring)

## AI & Edge Compute

### 2x NVIDIA Jetson Orin Nano Super Developer Kit

- GPU: 1024 CUDA cores, 32 Tensor cores
- CPU: 6-core ARM Cortex-A78AE @ 1.7GHz
- RAM: 8GB LPDDR5
- AI Performance: 67 TOPS (INT8)
- Network: Gigabit Ethernet
- Status: Unit 1 active, Unit 2 standby

## Available Equipment (Offline)

### MS-01 (i5-12600H) - Not Deployed

- CPU: Intel i5-12600H (12 cores, 16 threads)
- RAM: 96GB DDR5-5200
- Storage: 2TB NVMe
- Network: 10GbE + Thunderbolt 4
- Status: Offline to minimize power consumption
- Potential Use: 4th Ceph node, dedicated AI inference, test environment

## Network Topology

```
Internet (Comcast) → Motorola Modem
                          ↓
                    UDM Pro (Genesis) - 192.168.10.1
                   /                    \
          [10GbE SFP+]                [GbE Ports]
              ↓                            ↓
    USW Aggregation              Smart Home Devices
    192.168.10.182              (HA, Hue, Aqara)
    (8x 10GbE SFP+)
         |
         ├─ Port 1 → Node 1 (192.168.10.11)
         ├─ Port 5 → Node 2 (192.168.10.12)
         ├─ Port 6 → Node 3 (192.168.10.13)
         ├─ Port 7 → USW Pro HD 24 PoE
         └─ Port 8 → UDM Pro Uplink

    USW Pro HD 24 PoE - 192.168.10.99
    (24x GbE PoE + 2x 10G)
         |
         ├─ Port 21 → Dev Server (192.168.10.10) 1GbE
         ├─ Port 27 → Node 4 MS-A2 (192.168.10.14) 10GbE
         ├─ Ports 1,3,4,7,8,9 → Access Points (6 total)
         └─ Various → Smart Home, Services

    Synology DS1821+ - 192.168.10.159
         └─ 10GbE connection (LACP capable)
```

**Ceph Backend Network (Thunderbolt 4 Ring):**
```
┌─────────────────────────────────────────┐
│                                         │
│    Node 1 ←─ TB4 40Gbps ─→ Node 2     │
│   (.11)                      (.12)      │
│     ↑                          ↓        │
│     │                          │        │
│     └──── TB4 ←─ Node 3 ───────┘       │
│              (.13)                      │
│                                         │
│   Token-Ring Topology                  │
│   Dedicated Ceph Backend               │
└─────────────────────────────────────────┘
```

## IP Address Allocation

### Infrastructure

- 192.168.10.1 - UDM Pro (Genesis)
- 192.168.10.99 - USW Pro HD 24 PoE
- 192.168.10.182 - USW Aggregation
- 192.168.10.52 - USW Flex 2.5G 5
- 192.168.60.50 - Traefik Load Balancer (MetalLB)

### Compute Cluster

- 192.168.10.10 - Dev Server (KAMRUI i5-12450H)
- 192.168.10.11 - Node 1 (MS-01 i9-13900H) Ceph
- 192.168.10.12 - Node 2 (MS-01 i9-13900H) Ceph
- 192.168.10.13 - Node 3 (MS-01 i9-12900H) Ceph
- 192.168.10.14 - Node 4 (MS-A2 Ryzen 9 9955HX) Backup/DB
- 192.168.10.222 - Kubernetes API Server (thor cluster)

### Storage

- 192.168.10.159 - Synology DS1821+ (excelsior_nas.local)

### Kubernetes Services (via MetalLB)

- 192.168.60.50 - Traefik Load Balancer

### Access Points

- 192.168.10.53 - U6 Extender
- 192.168.10.81 - Roof AP
- 192.168.10.101 - Basement (PRO) AP
- 192.168.10.187 - 2nd Floor Office AP
- 192.168.10.234 - 2nd Floor Bedroom AP
- 192.168.10.249 - 1st Floor AP

### Smart Home

- 192.168.10.87 - Aqara Hub M3
- Home Assistant, Philips Hue Bridge (IPs TBD)

### Other Services

- 192.168.10.41 - Pi-Hole
- 192.168.10.225 - JetKVM

## Kubernetes Cluster: thor

### Cluster Details

**API Server:** https://192.168.10.222:6443
**Context Name:** `thor` or `kubernetes-admin@kubernetes`
**Version:** (check with `kubectl version`)

**Access:**
```bash
# Switch to thor cluster
kubectl config use-context thor

# Verify connection
kubectl cluster-info
kubectl get nodes
```

### Core Services Deployed

**Infrastructure Services:**

1. **Traefik** - Reverse Proxy / Ingress Controller
   - Namespace: `default` (or dedicated if moved)
   - Load Balancer IP: 192.168.60.50
   - Dashboard: https://traefik.xmojo.net (if configured)
   - Role: Routes external traffic to services
   - TLS: Wildcard certificate via cert-manager

2. **Authentik** - SSO / Identity Provider
   - Namespace: `authentik`
   - URL: https://auth.xmojo.net
   - Database: PostgreSQL (postgres-01.xmojo.net)
   - Role: Authentication for all services (Wiki, Plane, etc.)

3. **Wiki.js** - Documentation Platform
   - Namespace: `wiki`
   - URL: https://wiki.xmojo.net
   - Database: PostgreSQL (postgres-01.xmojo.net/wikijs)
   - API: https://wiki.xmojo.net/graphql
   - Role: Central documentation hub

4. **Plane** - Project Management
   - Namespace: (verify deployment location)
   - URL: https://plane.xmojo.net
   - Workspace: agile-solutions-group
   - Role: Task tracking for homelab and projects

5. **Cert-Manager** - TLS Certificate Management
   - Namespace: `cert-manager`
   - Role: Automatic TLS certificate generation
   - Issuer: Let's Encrypt (production)
   - Certificate: Wildcard `*.xmojo.net`

6. **MetalLB** - Load Balancer
   - Namespace: `metallb-system`
   - IP Pool: 192.168.60.50 (confirm range)
   - Role: Provides external IPs for services

7. **Rancher** - Kubernetes Management
   - (Document deployment details when confirmed)

8. **Grafana/Prometheus** - Monitoring
   - Namespace: `monitoring`
   - (Document URLs when confirmed)

9. **Headscale** - Self-hosted Tailscale Control Server
   - Namespace: `headscale`
   - Service URL: http://headscale.headscale.svc.cluster.local:8080
   - Role: VPN coordination server for Tailscale clients

10. **Headplane** - Web UI for Headscale
    - Namespace: `headscale`
    - URL: https://headplane.xmojo.net/admin/
    - Role: Web-based management interface for Headscale
    - Authentication: OIDC via Authentik
    - Features: User management, node management, pre-auth key generation

### Application Namespaces

- `smartwallet-local` - Smart Benefit Wallet development

### Automation

- `automation` - Credential secrets for Claude Code and automation

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

**Databases:**
- PostgreSQL (Authentik, Wiki.js, others)
- SQL Server (applications)

**Network:**
- UniFi (switching, routing, wireless)
- Cloudflare (DNS, edge proxy)

**Storage:**
- Synology DSM (NAS)
- Ceph (distributed block storage)

**Hardware:**
- Minisforum MS-01/MS-A2 (compute nodes)
- NVIDIA Jetson Orin Nano (edge AI)
- UniFi networking equipment

## Capacity and Limits

**Current Resources:**
- Total CPU: 172 cores (156 physical + 16 E-cores)
- Total RAM: 448GB (Proxmox cluster) + 32GB (Synology)
- Storage: 22TB (Synology) + ~6TB (Ceph, expanding to ~18TB)
- Network: 10GbE core, 120Gbps Ceph backend

**Before adding workloads, verify:**
- Available CPU/RAM capacity
- Storage space (especially Ceph)
- Network bandwidth
- Power availability
- Cooling capacity (AC unit status)

## Safety Constraints and Known Issues

### Critical Priorities

**⚠️ CRITICAL: AC Unit Electrical Issue**
- **Status:** Electrical issues preventing AC unit operation
- **Impact:** Cannot increase compute density until resolved
- **Risk:** Equipment thermal throttling and potential damage
- **Action Required:** Fix electrical wiring/circuit before major expansions

**Power Management:**
- **Philosophy:** Keep idle equipment offline to minimize power draw
- **Active Equipment:** Nodes 1-4, Dev Server, network gear
- **Offline Equipment:** MS-01 i5-12600H (standby)
- **Monitor:** Power consumption and thermal status

### Known Issues

**High Priority:**

- ⚠️ **AC Unit Electrical:** Resolve wiring/circuit issues (CRITICAL)
- ⚠️ **USW Aggregation Port 2:** SFP signal loss - clean up or fix
- ⚠️ **USW Aggregation Port 3:** Connected but no traffic - investigate
- ⚠️ **Node 2 Hostname:** Shows as "shellyswitch25-68C6" (needs correction)
- ⚠️ **Dev Server Hostname:** Shows as "proxmox-01" (conflicts with cluster naming)

**Medium Priority:**

- Storage expansion planning (3x 4TB NVMe for Ceph)
- UPS capacity evaluation and potential second UPS
- Documentation completeness (service inventory, procedures)
- Monitoring/observability stack deployment

**Low Priority:**

- Aqara door lock installation
- Access Point model documentation
- GPU options research for MS-01 nodes

## Team and Access

**Primary Administrator:** Jay Stuart

**Access Levels:**
- Kubernetes cluster admin: (document who has access)
- Proxmox admin: (document who has access)
- Network admin (UniFi): (document who has access)
- Service admins: (document per-service access)

**Emergency Contacts:**
- Primary: (document contact info)
- Backup: (document contact info)

**Escalation Procedures:**
- Critical infrastructure failure: (document procedure)
- Security incident: (document procedure)
- Data loss: (document procedure)
