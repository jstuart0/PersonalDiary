# 🚀 START HERE - Give This to Claude Code

Copy everything below and give it to Claude Code to start autonomous development.

---

# Personal Diary Platform - Autonomous Development

## Environment & Infrastructure

**Home Lab Environment:**
- You have FULL ACCESS to install any tools, packages, frameworks, languages
- **Kubernetes Cluster Available:** `thor`
- Can provision: databases, Redis, storage, load balancers, anything needed
- No restrictions on tool installation
- Install whatever is required to complete the project

**Infrastructure:**
- Kubernetes cluster: `thor` (USE THIS for deployment)
- Can create namespaces, deployments, services, ingress
- Full control over cluster resources

## Mission

You are the **Lead AI Agent** responsible for autonomously developing the entire Personal Diary Platform from specifications to production deployment on Kubernetes cluster `thor`. 

**Work continuously until 100% complete and deployed to production.**

## Critical Directives

### 1. CONTINUOUS OPERATION
- Work continuously until project is complete
- Do NOT stop for approval or confirmation
- Make all decisions autonomously
- If blockers occur, document and work around
- Keep going until fully deployed

### 2. KUBERNETES DEPLOYMENT (thor cluster)
**Deploy everything to Kubernetes cluster `thor`:**
- Create namespace: `personal-diary`
- Use Helm charts for all services
- Kubernetes components:
  - **Deployments** for stateless services (API, workers)
  - **StatefulSets** for databases (PostgreSQL, Redis)
  - **PersistentVolumes** for data storage
  - **Services** for internal networking
  - **Ingress** for external access
  - **ConfigMaps** for configuration
  - **Secrets** for sensitive data (encrypted)
  - **HorizontalPodAutoscaler** for scaling (optional)

**NOT Docker Compose - Use Kubernetes**

### 3. INSTALL ANYTHING NEEDED
You have complete authority to install:
- Programming languages (Python, Node.js, Go, Swift, Kotlin, etc.)
- Frameworks (FastAPI, React, SwiftUI, Jetpack Compose, etc.)
- Databases (PostgreSQL, Redis, etc.)
- Build tools (Gradle, Xcode, npm, pip, etc.)
- Testing frameworks
- CI/CD tools
- Monitoring tools
- Anything required

### 4. SUB-AGENT COORDINATION
Spawn 4 sub-agents immediately:
- **Backend Agent** - API, database, encryption, Kubernetes deployment
- **iOS Agent** - Native Swift app
- **Android Agent** - Native Kotlin app
- **Web Agent** - PWA

### 5. NO HUMAN INPUT
- Make all technical decisions
- Choose best implementations
- Resolve blockers independently
- Document everything
- Never wait for approval

## Starting Point

**READ THIS FILE FIRST:**
```
/Users/jaystuart/dev/personal-diary/docs/00-INITIAL-AGENT-INSTRUCTIONS.md
```

This contains:
- Complete development workflow
- Sub-agent coordination
- Progress tracking system
- Git workflow (jstuart0/PersonalDiary)
- Quality standards
- Testing requirements

## Then Read These (In Order):

1. `/Users/jaystuart/dev/personal-diary/docs/00-CONTEXT-HANDOFF.md`
2. `/Users/jaystuart/dev/personal-diary/docs/01-project-specification.md`
3. `/Users/jaystuart/dev/personal-diary/docs/02-ai-agent-implementation-guide.md`
4. `/Users/jaystuart/dev/personal-diary/docs/03-ios-app-specification.md`
5. `/Users/jaystuart/dev/personal-diary/docs/04-android-app-specification.md`
6. `/Users/jaystuart/dev/personal-diary/docs/05-web-app-specification.md`
7. `/Users/jaystuart/dev/personal-diary/docs/06-search-implementation.md`

## What You Will Build

**Backend (Python + FastAPI):**
- Dual-tier encryption (E2E + UCE)
- RESTful API
- PostgreSQL database
- Redis caching
- Celery background jobs
- S3-compatible storage
- Facebook integration
- Search (PostgreSQL FTS → Elasticsearch)
- **Deployed to Kubernetes**

**iOS App (Swift + SwiftUI):**
- Native iOS 16+
- Client-side encryption
- Keychain integration
- Offline-first
- Biometric auth

**Android App (Kotlin + Jetpack Compose):**
- Native Android 9+
- Client-side encryption
- KeyStore integration
- Offline-first
- Material Design 3

**Web App (React/Vue/Svelte):**
- Progressive Web App
- Web Crypto API
- IndexedDB
- Service Worker
- Installable, offline-capable

**Infrastructure (Kubernetes):**
- Helm charts
- Deployments & StatefulSets
- Services & Ingress
- ConfigMaps & Secrets
- Monitoring & logging

## First Actions (Execute Immediately)

### 1. Read Specifications (30 min)
Read all 8 specification documents

### 2. Create Progress Tracking (15 min)
- Create `PROGRESS.md`
- Create `SUB_AGENTS.md`
- Create `DECISIONS.md`

### 3. Make Technology Decisions (30 min)
Choose:
- Backend language (Python/Node.js/Go)
- Mobile approach (Native recommended)
- Web framework (React/Vue/Svelte)
- Document in `DECISIONS.md`

### 4. Initialize Repository (15 min)
```bash
git clone https://github.com/jstuart0/PersonalDiary.git
cd PersonalDiary
# Create branch structure
# Create project structure
# Initial commit
```

### 5. Set Up Kubernetes (30 min)
```bash
kubectl create namespace personal-diary
# Deploy PostgreSQL StatefulSet
# Deploy Redis Deployment
# Create PersistentVolumes
# Configure Ingress
```

### 6. Spawn Sub-Agents (10 min)
Create 4 parallel sub-agents

### 7. Begin Development (Continuous)
Start implementing all components in parallel

## Success Criteria

### Must Achieve:
- [ ] All services deployed to Kubernetes `thor`
- [ ] Both encryption tiers working (E2E + UCE)
- [ ] All platforms functional (iOS, Android, Web)
- [ ] Facebook integration working
- [ ] Multi-device sync working
- [ ] Search working (tier-appropriate)
- [ ] 80%+ test coverage (all components)
- [ ] Zero security vulnerabilities
- [ ] Performance targets met
- [ ] iOS in TestFlight
- [ ] Android in Play Console internal testing
- [ ] Web deployed and accessible
- [ ] Wiki complete

## Timeline

**Work continuously through phases:**
- Phase 1: Foundation (Weeks 1-2)
- Phase 2: Core Features (Weeks 3-4)
- Phase 3: Social Integration (Weeks 5-6)
- Phase 4: Sync & Polish (Weeks 7-8)
- Phase 5: Testing & Deployment (Weeks 9-10)

**~10 weeks of autonomous development**

## Important Reminders

### Security (Non-Negotiable)
- E2E: keys NEVER on server
- UCE: master keys encrypted at rest
- No sensitive data in logs
- Input validation everywhere
- HTTPS/TLS everywhere

### Quality (Non-Negotiable)
- 80%+ test coverage
- Security-first always
- Performance targets met
- Clean, maintainable code
- Comprehensive documentation

### Kubernetes Deployment
- Use `thor` cluster
- Namespace: `personal-diary`
- Helm charts for everything
- Follow Kubernetes best practices
- StatefulSets for databases
- ConfigMaps for config
- Secrets for sensitive data

### Decision Making
- Choose RECOMMENDED solutions from specs
- If no recommendation, choose BEST implementation
- Document ALL decisions in `DECISIONS.md`
- Never compromise on security or quality

### Don't Stop
- Work continuously
- No approval needed
- Resolve blockers independently
- Document and continue
- Only stop when 100% deployed

## Your Authority

You have **COMPLETE AUTHORITY** to:
- Install any tools/packages/frameworks
- Make all technical decisions
- Choose implementations
- Modify architecture (if improvements found)
- Deploy to Kubernetes
- Create CI/CD pipelines
- Configure monitoring
- Everything needed to complete

## Git Repository

**Repository:** https://github.com/jstuart0/PersonalDiary

**Commit frequently:**
- Every 2-4 hours
- After major milestones
- Before stopping
- Push all commits

## Final Instruction

**BEGIN IMMEDIATELY.**

**WORK CONTINUOUSLY.**

**DO NOT STOP UNTIL FULLY DEPLOYED TO KUBERNETES CLUSTER `thor`.**

Read `/Users/jaystuart/dev/personal-diary/docs/00-INITIAL-AGENT-INSTRUCTIONS.md` first, then execute the complete development and deployment autonomously.

🚀 **START NOW**

---

# End of Prompt

Give everything above (from "# Personal Diary Platform" to "START NOW") to Claude Code.
