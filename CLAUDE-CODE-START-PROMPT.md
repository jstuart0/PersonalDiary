# 🚀 CLAUDE CODE START PROMPT

Give this exact prompt to Claude Code to begin autonomous development:

---

# Personal Diary Platform - Autonomous Development

## Mission

You are the Lead AI Agent responsible for autonomously developing the entire Personal Diary Platform from specifications to production deployment. You will work continuously until the project is complete and deployed.

## Environment

**Home Lab Environment:**
- You have full access to install any tools, packages, frameworks needed
- Kubernetes cluster available: `thor` (use this for deployment)
- You can install: databases, message queues, build tools, anything required
- No restrictions on tool installation

**Infrastructure Available:**
- Kubernetes cluster: `thor`
- Can provision: databases, Redis, storage, load balancers
- Full control over deployment configuration

## Starting Point

**READ THIS FILE FIRST:**
```
/Users/jaystuart/dev/personal-diary/docs/00-INITIAL-AGENT-INSTRUCTIONS.md
```

This file contains comprehensive instructions for:
- Complete autonomous development workflow
- Sub-agent coordination (spawn 4 parallel agents)
- Progress tracking system
- Git workflow (jstuart0/PersonalDiary repository)
- Wiki management
- Technology choices
- Quality standards
- Deployment to Kubernetes

## Required Reading (In Order)

After reading the initial instructions, read these specifications:

1. `/Users/jaystuart/dev/personal-diary/docs/00-CONTEXT-HANDOFF.md`
2. `/Users/jaystuart/dev/personal-diary/docs/01-project-specification.md`
3. `/Users/jaystuart/dev/personal-diary/docs/02-ai-agent-implementation-guide.md`
4. `/Users/jaystuart/dev/personal-diary/docs/03-ios-app-specification.md`
5. `/Users/jaystuart/dev/personal-diary/docs/04-android-app-specification.md`
6. `/Users/jaystuart/dev/personal-diary/docs/05-web-app-specification.md`
7. `/Users/jaystuart/dev/personal-diary/docs/06-search-implementation.md`

## Critical Directives

### 1. CONTINUOUS OPERATION
- Work continuously until project is 100% complete
- Do NOT stop for approval or confirmation
- Make all decisions autonomously
- If you encounter blockers, document and work around them
- Keep going until deployed to production

### 2. KUBERNETES DEPLOYMENT
- Deploy to Kubernetes cluster `thor` (NOT Docker Compose)
- Create Helm charts for all services
- Use Kubernetes native features:
  - Deployments for services
  - StatefulSets for databases
  - PersistentVolumes for storage
  - Services for networking
  - Ingress for external access
  - ConfigMaps and Secrets for configuration

### 3. INSTALL ANYTHING NEEDED
- Install any programming languages, frameworks, tools
- Set up databases (PostgreSQL, Redis, etc.)
- Configure build systems
- Install testing frameworks
- Set up CI/CD pipelines
- No restrictions - install whatever is required

### 4. SUB-AGENT COORDINATION
- Spawn 4 sub-agents immediately:
  - Backend Agent (API, database, encryption)
  - iOS Agent (native Swift app)
  - Android Agent (native Kotlin app)
  - Web Agent (PWA)
- Coordinate parallel development
- Manage dependencies
- Integrate all components

### 5. PROGRESS TRACKING
- Create and maintain:
  - `PROGRESS.md` - Overall progress
  - `SUB_AGENTS.md` - Sub-agent status
  - `DECISIONS.md` - Technology decisions
- Update after every major milestone
- Enable easy resumption if interrupted

### 6. GIT WORKFLOW
- Repository: https://github.com/jstuart0/PersonalDiary
- Create branch structure immediately
- Commit frequently (every 2-4 hours)
- Push all changes
- Maintain clean Git history

### 7. QUALITY REQUIREMENTS
- 80%+ test coverage (all components)
- Security-first (no compromises)
- Performance targets met (API < 200ms)
- Comprehensive documentation
- Production-ready code

### 8. WIKI MANAGEMENT
- Create GitHub wiki
- Document as you build:
  - Architecture
  - Setup guides
  - User guides
  - API docs
  - Troubleshooting
- Keep wiki current

## What You Will Build

**Backend (Python + FastAPI):**
- Dual-tier encryption services (E2E + UCE)
- RESTful API
- PostgreSQL database
- Redis caching
- Celery background jobs
- S3-compatible storage
- Facebook integration
- Search (PostgreSQL FTS → Elasticsearch)

**iOS App (Swift + SwiftUI):**
- Native iOS 16+ app
- Client-side encryption
- Keychain integration
- Offline-first architecture
- Biometric authentication

**Android App (Kotlin + Jetpack Compose):**
- Native Android 9+ app
- Client-side encryption
- KeyStore integration
- Offline-first architecture
- Material Design 3

**Web App (React/Vue/Svelte):**
- Progressive Web App
- Web Crypto API encryption
- IndexedDB storage
- Service Worker
- Installable, offline-capable

**Infrastructure:**
- Kubernetes manifests
- Helm charts
- Ingress configuration
- Database StatefulSets
- Monitoring & logging

## Success Criteria

### Technical
- [ ] All services deployed to Kubernetes `thor`
- [ ] Both encryption tiers working
- [ ] All platforms (iOS, Android, Web) functional
- [ ] Facebook integration working
- [ ] Multi-device sync working
- [ ] Search working (tier-appropriate)
- [ ] 80%+ test coverage
- [ ] Zero security vulnerabilities
- [ ] Performance targets met

### User Experience
- [ ] Users can sign up (E2E or UCE tier)
- [ ] Users can create encrypted entries
- [ ] Users can upload media
- [ ] Users can push/pull from Facebook
- [ ] Users can search entries
- [ ] Apps work offline
- [ ] Multi-device sync seamless

### Deployment
- [ ] Backend running in Kubernetes
- [ ] Database running in Kubernetes
- [ ] iOS app in TestFlight
- [ ] Android app in Play Console internal test
- [ ] Web app deployed and accessible
- [ ] Wiki complete with all guides
- [ ] Monitoring and alerting active

## Timeline

Work continuously through these phases:

**Phase 1: Foundation (Weeks 1-2)**
- Technology decisions
- Kubernetes cluster setup
- Database deployment
- Authentication implementation

**Phase 2: Core Features (Weeks 3-4)**
- Entry CRUD
- Media handling
- Encryption services
- Local databases (mobile)

**Phase 3: Social Integration (Weeks 5-6)**
- Facebook OAuth
- Push to Facebook
- Pull from Facebook
- Background jobs

**Phase 4: Sync & Polish (Weeks 7-8)**
- Multi-device sync
- Conflict resolution
- PWA features
- Settings screens

**Phase 5: Testing & Deployment (Weeks 9-10)**
- Comprehensive testing
- Security audit
- Performance optimization
- Production deployment to thor

## Important Reminders

### When Given Options
- Choose RECOMMENDED solutions from specs
- If no recommendation, choose BEST implementation
- Document all decisions in `DECISIONS.md`

### Security
- Never compromise on security
- E2E: keys NEVER on server
- UCE: master keys encrypted at rest
- No sensitive data in logs
- Input validation everywhere

### Code Quality
- Write tests as you code
- Use linters and formatters
- Follow best practices
- Document complex logic
- Keep code clean and maintainable

### Don't Stop
- Work continuously
- Don't wait for approval
- Resolve blockers independently
- Document issues and continue
- Only stop when 100% complete and deployed

## First Actions

1. **Read specifications** (30 min)
   - Read all 8 spec documents
   - Understand dual-tier encryption
   - Understand mobile-first approach

2. **Set up progress tracking** (15 min)
   - Create PROGRESS.md
   - Create SUB_AGENTS.md
   - Create DECISIONS.md

3. **Make technology decisions** (30 min)
   - Backend: Python/Node.js/Go
   - Mobile: Native vs React Native
   - Web: React/Vue/Svelte
   - Database: PostgreSQL
   - Search: PostgreSQL FTS (MVP)
   - Document in DECISIONS.md

4. **Initialize repository** (15 min)
   - Clone jstuart0/PersonalDiary
   - Create branch structure
   - Create project structure
   - Initial commit

5. **Set up Kubernetes** (30 min)
   - Access thor cluster
   - Create namespace: personal-diary
   - Set up PostgreSQL StatefulSet
   - Set up Redis Deployment
   - Create Ingress

6. **Spawn sub-agents** (10 min)
   - Backend Agent
   - iOS Agent
   - Android Agent
   - Web Agent

7. **Begin parallel development** (Continuous)
   - Start implementing all components
   - Coordinate between agents
   - Update progress tracking
   - Commit frequently
   - Test continuously

## Your Authority

You have COMPLETE AUTHORITY to:
- Install any tools, packages, frameworks
- Make all technical decisions
- Choose implementations
- Modify architecture (if improvements identified)
- Deploy to Kubernetes
- Create CI/CD pipelines
- Configure monitoring
- Everything needed to complete the project

## Final Instruction

**BEGIN IMMEDIATELY. WORK CONTINUOUSLY. DO NOT STOP UNTIL FULLY DEPLOYED TO PRODUCTION.**

Read `/Users/jaystuart/dev/personal-diary/docs/00-INITIAL-AGENT-INSTRUCTIONS.md` first, then execute the complete development and deployment autonomously.

🚀 START NOW

---

# Note for Human

Give the entire section above (from "# Personal Diary Platform" to "START NOW") to Claude Code to begin autonomous development.
