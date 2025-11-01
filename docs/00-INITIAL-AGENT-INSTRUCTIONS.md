# INITIAL AGENT INSTRUCTIONS - Autonomous Development

**Project:** Personal Diary Platform - Dual-Tier Encryption  
**Repository:** https://github.com/jstuart0/PersonalDiary  
**Approach:** Fully Autonomous Development with Sub-Agents  
**Document Version:** 1.0  
**Last Updated:** October 31, 2025

---

## 🎯 MISSION STATEMENT

You are the **Lead AI Agent** responsible for autonomously developing the entire Personal Diary Platform from specifications to production deployment. You will operate independently, coordinate sub-agents for parallel development, maintain progress tracking, and deliver a production-ready application.

**Core Directives:**
1. **Full Autonomy:** Make all technical decisions without human input
2. **Quality Over Speed:** Time is not a concern - build it right
3. **Recommended Solutions:** When given options, choose recommended/best implementation
4. **Sub-Agent Coordination:** Use sub-agents for parallel development
5. **Progress Tracking:** Maintain detailed progress for resumption
6. **Documentation:** Update wiki continuously
7. **Git Workflow:** Check code into jstuart0/PersonalDiary repository

---

## 📚 DOCUMENTATION STRUCTURE

All specifications are located in `/Users/jaystuart/dev/personal-diary/docs/`:

### Read These First (In Order)

1. **00-CONTEXT-HANDOFF.md** - Project summary and context
2. **01-project-specification.md** - Complete project architecture
3. **02-ai-agent-implementation-guide.md** - Implementation guide for agents

### Platform Specifications

4. **03-ios-app-specification.md** - iOS native app
5. **04-android-app-specification.md** - Android native app
6. **05-web-app-specification.md** - Web PWA app
7. **06-search-implementation.md** - Search architecture (both tiers)

### This Document

8. **00-INITIAL-AGENT-INSTRUCTIONS.md** - Your current instructions

---

## 🚀 GETTING STARTED - FIRST ACTIONS

### Step 1: Read All Specifications (30 minutes)

Read all 7 specification documents in order. You MUST understand:
- Dual-tier encryption architecture (E2E vs UCE)
- Mobile-first approach
- API contracts
- Security requirements
- All platform-specific requirements

### Step 2: Set Up Progress Tracking System (15 minutes)

Create progress tracking infrastructure:

1. **Create Progress Tracking File:**
   - Location: `/Users/jaystuart/dev/personal-diary/PROGRESS.md`
   - Format: Markdown with checkboxes
   - Structure: See template below

2. **Create Sub-Agent Registry:**
   - Location: `/Users/jaystuart/dev/personal-diary/SUB_AGENTS.md`
   - Track: Active agents, assigned tasks, status

3. **Create Decision Log:**
   - Location: `/Users/jaystuart/dev/personal-diary/DECISIONS.md`
   - Document: All technical decisions and rationale

### Step 3: Make Technology Decisions (30 minutes)

Choose technologies for all components:

**Backend:**
- Language: Python / Node.js / Go
- Framework: FastAPI / Express / Gin
- Database: PostgreSQL (recommended)
- Task Queue: Celery / Bull
- Storage: AWS S3 (recommended)

**Mobile:**
- iOS: Swift + SwiftUI (native recommended)
- Android: Kotlin + Jetpack Compose (native recommended)
- Alternative: React Native (for code reuse)

**Web:**
- Framework: React / Vue / Svelte
- Build Tool: Vite (recommended)

**Search:**
- Start with PostgreSQL FTS (recommended)
- Plan migration to Elasticsearch later

**Document ALL decisions in DECISIONS.md with rationale.**

### Step 4: Initialize Git Repository (15 minutes)

1. Clone repository:
   ```
   git clone https://github.com/jstuart0/PersonalDiary.git
   cd PersonalDiary
   ```

2. Create branch structure:
   ```
   main (protected)
   ├── develop (integration branch)
   ├── backend/feature-* (backend features)
   ├── ios/feature-* (iOS features)
   ├── android/feature-* (Android features)
   └── web/feature-* (Web features)
   ```

3. Create initial project structure:
   ```
   PersonalDiary/
   ├── backend/
   ├── ios/
   ├── android/
   ├── web/
   ├── docs/ (copy specifications here)
   ├── PROGRESS.md
   ├── SUB_AGENTS.md
   ├── DECISIONS.md
   └── README.md
   ```

4. Initial commit:
   ```
   git add .
   git commit -m "Initial project structure and specifications"
   git push origin develop
   ```

### Step 5: Create Sub-Agents (10 minutes)

Spawn 4 parallel sub-agents:

1. **Backend Agent** - API, database, encryption services
2. **iOS Agent** - iOS native app
3. **Android Agent** - Android native app
4. **Web Agent** - Web PWA app

Each sub-agent gets:
- Specific specification documents
- Clear scope and deliverables
- Access to shared PROGRESS.md
- Git branch naming convention

### Step 6: Begin Implementation (Start)

Start parallel development across all sub-agents.

---

## 📊 PROGRESS TRACKING SYSTEM

### PROGRESS.md Template

Create this file with the following structure:

```markdown
# Personal Diary Platform - Development Progress

**Last Updated:** [ISO timestamp]  
**Lead Agent:** [Your identifier]  
**Status:** [Phase name]  
**Completion:** X%

---

## 🎯 Current Sprint

**Sprint:** [Number and name]  
**Start:** [Date]  
**Target End:** [Date]  
**Focus:** [Brief description]

### Active Sub-Agents

- **Backend Agent:** [Status] - [Current task]
- **iOS Agent:** [Status] - [Current task]
- **Android Agent:** [Status] - [Current task]
- **Web Agent:** [Status] - [Current task]

---

## ✅ Completed Milestones

### Phase 1: Foundation
- [x] Technology decisions made
- [x] Repository initialized
- [x] Project structure created
- [ ] Database schema designed
- [ ] API contracts defined
...

### Phase 2: Core Features
- [ ] User authentication
- [ ] Entry CRUD operations
...

---

## 🔄 In Progress

### Backend (Agent: backend-01)
- [ ] User authentication endpoints (30%)
- [ ] Encryption services (10%)

### iOS (Agent: ios-01)
- [ ] Project setup (50%)
- [ ] Signup screens (0%)

...

---

## ⏸️ Blocked Items

1. **Facebook App Review**
   - Blocker: Need business verification
   - Required for: Social media integration
   - Action: [Who/What]

---

## 📝 Next Actions

1. Complete database schema (Backend Agent)
2. Finish iOS project setup (iOS Agent)
3. ...

---

## 📈 Metrics

- **Total Tasks:** X
- **Completed:** X
- **In Progress:** X
- **Blocked:** X
- **Lines of Code:** X
- **Test Coverage:** X%
```

**Update this file:**
- After every major milestone
- At least once per day
- Before stopping work (for resumption)
- When sub-agents complete tasks

---

## 🤖 SUB-AGENT COORDINATION

### Creating Sub-Agents

When creating a sub-agent:

1. **Define Clear Scope:**
   - Specific component (Backend/iOS/Android/Web)
   - Clear deliverables
   - Success criteria

2. **Provide Resources:**
   - Relevant specification documents
   - Technology decisions
   - API contracts (if needed)
   - Access to PROGRESS.md

3. **Assign Identifier:**
   - Format: `[component]-[number]`
   - Example: `backend-01`, `ios-01`, `android-01`, `web-01`

4. **Register in SUB_AGENTS.md:**
   ```markdown
   ## Active Sub-Agents
   
   ### backend-01
   - **Component:** Backend API
   - **Started:** 2025-10-31T12:00:00Z
   - **Status:** Active
   - **Current Task:** User authentication
   - **Branch:** backend/auth-system
   - **Last Update:** 2025-10-31T15:30:00Z
   ```

### Communication Protocol

**Sub-agents report to Lead Agent:**
- Progress updates (every 2-4 hours)
- Blockers immediately
- Completion of milestones
- Questions/clarifications

**Lead Agent responsibilities:**
- Resolve blockers
- Make cross-component decisions
- Coordinate dependencies
- Merge work from sub-agents

### Parallel Development Strategy

**Independent Work (High Priority):**
- Backend API development
- iOS app development
- Android app development
- Web app development

**Dependencies to Manage:**
- Backend must define API contracts early
- Clients wait for API contracts before full integration
- But clients can build UI/local features in parallel

**Coordination Points:**
- Weekly sync: Review progress, resolve conflicts
- API contract changes: Notify all client agents
- Database schema changes: Update all agents

---

## 💾 GIT WORKFLOW

### Branch Strategy

**Main Branches:**
- `main` - Production-ready code (protected)
- `develop` - Integration branch (protected)

**Feature Branches:**
- `backend/feature-name` - Backend features
- `ios/feature-name` - iOS features
- `android/feature-name` - Android features
- `web/feature-name` - Web features

**Examples:**
- `backend/user-authentication`
- `backend/encryption-services`
- `ios/signup-flow`
- `android/timeline-ui`
- `web/pwa-setup`

### Commit Guidelines

**Commit Message Format:**
```
[component] Brief description

Detailed description of changes

- Change 1
- Change 2

Refs: #issue-number (if applicable)
```

**Examples:**
```
[backend] Implement user authentication endpoints

Added signup, login, and token refresh endpoints.
Includes JWT generation and validation.

- POST /auth/signup
- POST /auth/login
- POST /auth/refresh

Refs: #12
```

### Pull Request Process

**When to Create PR:**
- Feature complete
- Tests passing
- Documentation updated

**PR Template:**
```markdown
## Description
[What does this PR do?]

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style
- [ ] Tests passing
- [ ] Documentation updated
- [ ] No sensitive data in code
```

**Review Process:**
- Lead Agent reviews all PRs
- Sub-agents can review each other's PRs (optional)
- Automated tests must pass
- Merge to `develop` first
- Merge to `main` for releases

### Commit Frequency

**Commit often:**
- After completing a logical unit of work
- Before switching contexts
- At least once per 2-4 hours of work
- Before stopping work (for resumption)

**Push regularly:**
- After every commit
- Enables resumption by another agent
- Backs up work

---

## 📖 WIKI MANAGEMENT

### Wiki Structure

Create these wiki pages on GitHub:

**Home Page:**
- Project overview
- Quick start guide
- Links to all other pages

**Architecture:**
- System architecture diagrams
- Technology stack
- Design decisions

**Setup Guides:**
- Backend setup
- iOS setup
- Android setup
- Web setup
- Development environment

**User Guides:**
- How to sign up (E2E vs UCE)
- How to create entries
- How to share to Facebook
- How to import from Facebook
- How to search

**API Documentation:**
- Authentication
- Entries
- Search
- Social media integration
- Media handling

**Development Guides:**
- Contributing guidelines
- Code style guide
- Testing guide
- Deployment guide

**Troubleshooting:**
- Common issues
- Error messages
- FAQ

**Release Notes:**
- Version history
- Changelog
- Migration guides

### Update Schedule

**Update wiki:**
- When implementing new features
- When changing architecture
- When adding new APIs
- After major milestones
- Before each release

**Wiki Maintenance:**
- Keep documentation current
- Add screenshots/diagrams
- Include code examples
- Link related pages
- Use consistent formatting

---

## 🎯 IMPLEMENTATION PRIORITIES

### Phase 1: Foundation (Weeks 1-2)

**Priority: HIGH**

**Backend:**
1. Database schema implementation
2. User authentication (both tiers)
3. Encryption services (E2E and UCE)
4. Entry CRUD APIs
5. Docker setup

**iOS:**
1. Project setup
2. Authentication screens
3. Encryption tier selection
4. Keychain service
5. Recovery codes flow

**Android:**
1. Project setup
2. Authentication screens
3. Encryption tier selection
4. KeyStore manager
5. Recovery codes flow

**Web:**
1. Project setup (Vite + chosen framework)
2. Authentication pages
3. Encryption tier selection
4. Web Crypto setup
5. IndexedDB service

**Deliverables:**
- Users can sign up (both tiers)
- Users can log in
- Basic infrastructure in place

---

### Phase 2: Core Features (Weeks 3-4)

**Priority: HIGH**

**Backend:**
1. Media upload/download
2. Tag system
3. Entry events (audit trail)
4. Sync endpoints
5. Search implementation (PostgreSQL FTS)

**iOS:**
1. Timeline screen
2. Entry detail screen
3. Create/edit entry
4. Media capture/upload
5. Local database (Core Data/SwiftData)

**Android:**
1. Timeline screen
2. Entry detail screen
3. Create/edit entry
4. Media capture/upload
5. Local database (Room)

**Web:**
1. Timeline page
2. Entry detail page
3. Create/edit entry
4. Media upload
5. Search UI

**Deliverables:**
- Users can create encrypted entries
- Users can upload media
- Users can view timeline
- Search works (tier-appropriate)

---

### Phase 3: Social Integration (Weeks 5-6)

**Priority: MEDIUM**

**Backend:**
1. Facebook OAuth endpoints
2. Facebook push implementation
3. Facebook pull implementation
4. Background jobs (Celery/Bull)
5. Deduplication logic

**All Clients:**
1. Facebook OAuth flow
2. Share to Facebook UI
3. Import from Facebook UI
4. External post indicators

**Deliverables:**
- Users can connect Facebook
- Users can push entries to Facebook
- Users can pull posts from Facebook

---

### Phase 4: Sync & Polish (Weeks 7-8)

**Priority: MEDIUM**

**Backend:**
1. Optimize sync endpoints
2. WebSocket support (optional)
3. Performance tuning
4. Monitoring setup

**iOS:**
1. Background sync
2. Conflict resolution
3. Settings screen
4. Animations/polish

**Android:**
1. WorkManager sync
2. Conflict resolution
3. Settings screen
4. Animations/polish

**Web:**
1. Service Worker
2. PWA manifest
3. Offline mode
4. Install prompt

**Deliverables:**
- Reliable sync across devices
- Offline mode works
- PWA installable
- Production-ready apps

---

### Phase 5: Testing & Deployment (Weeks 9-10)

**Priority: HIGH**

**All Components:**
1. Unit test coverage > 80%
2. Integration tests
3. E2E tests
4. Security audit
5. Performance testing
6. Documentation complete

**Deployment:**
1. Backend: Deploy to production
2. iOS: TestFlight beta
3. Android: Internal testing track
4. Web: Production deployment

**Deliverables:**
- Comprehensive test coverage
- Production deployment
- Beta testing programs live
- Documentation complete

---

## 🛠️ TECHNICAL DECISION FRAMEWORK

### When to Make Decisions

You make decisions on:
- Technology choices (when options given)
- Implementation details
- Architecture patterns
- Testing strategies
- Deployment approaches
- Performance optimizations

### Decision Criteria

**Always prioritize:**
1. **Recommended solutions** in specifications
2. **Best implementation** for the use case
3. **Security** - never compromise
4. **Quality** over speed
5. **Maintainability** - clean code, tests, docs
6. **Scalability** - plan for growth

**When choosing between options:**
1. Check if one is "recommended" in specs
2. Evaluate pros/cons
3. Consider long-term maintainability
4. Check ecosystem support
5. Document decision and rationale
6. Proceed with confidence

### Example Decisions

**Backend Language:**
- Options: Python, Node.js, Go
- Recommendation: Python (FastAPI)
- Your Choice: Python
- Rationale: Recommended, excellent async, strong typing, good ecosystem

**Mobile Approach:**
- Options: Native (Swift/Kotlin) or React Native
- Recommendation: Native
- Your Choice: Native
- Rationale: Best performance, best UX, platform-specific features

**Search (MVP):**
- Options: PostgreSQL FTS or Elasticsearch
- Recommendation: PostgreSQL FTS
- Your Choice: PostgreSQL FTS
- Rationale: Simpler for MVP, can migrate later

**Document every decision in DECISIONS.md.**

---

## 🔐 SECURITY CHECKLIST

### Must-Haves (Non-Negotiable)

- [ ] All data encrypted at rest
- [ ] HTTPS/TLS for all communication
- [ ] E2E keys never leave client
- [ ] UCE master keys encrypted
- [ ] No sensitive data in logs
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention (parameterized queries)
- [ ] XSS prevention (sanitize outputs)
- [ ] CSRF protection
- [ ] Rate limiting
- [ ] Password hashing (Bcrypt/Argon2)
- [ ] JWT token management
- [ ] Keychain/KeyStore for mobile
- [ ] Web Crypto API for web
- [ ] No secrets in code/git

### Security Testing

- [ ] Automated security scanning
- [ ] Penetration testing (optional)
- [ ] Code review for vulnerabilities
- [ ] Dependency vulnerability scanning
- [ ] OWASP Top 10 compliance

---

## 🧪 TESTING REQUIREMENTS

### Test Coverage Targets

**Minimum Coverage:**
- Backend: 80%+
- iOS: 80%+
- Android: 80%+
- Web: 80%+

**Test Types:**

**Unit Tests:**
- Test individual functions/methods
- Test edge cases
- Test error handling
- Fast execution

**Integration Tests:**
- Test API endpoints
- Test database operations
- Test service interactions
- Test encryption/decryption

**E2E Tests:**
- Test complete user flows
- Test across all platforms
- Test critical paths
- Automated where possible

### Testing Strategy

**Test as you build:**
- Write tests before or alongside code (TDD/BDD)
- Run tests before every commit
- Fix failing tests immediately
- Don't compromise on test coverage

**Continuous Testing:**
- Automated tests in CI/CD
- Run tests on every PR
- Block merges if tests fail
- Monitor test performance

---

## 📊 QUALITY METRICS

### Code Quality

**Measure:**
- Code coverage (> 80%)
- Code complexity (keep low)
- Code duplication (minimize)
- Code review feedback

**Tools:**
- Linters (ESLint, pylint, SwiftLint, ktlint)
- Formatters (Prettier, Black, SwiftFormat)
- Static analysis (SonarQube optional)

### Performance

**Backend:**
- API response time < 200ms (p95)
- Database query time < 50ms (p95)
- Concurrent users: 1000+

**Mobile:**
- App launch time < 2 seconds
- UI responsiveness: 60 FPS
- Memory usage < 150MB
- Battery efficient

**Web:**
- Lighthouse score > 90
- First Contentful Paint < 1.5s
- Time to Interactive < 3.5s

---

## 🚨 INTERRUPTION & RESUMPTION

### Before Interruption (End of Session)

**Always do these before stopping:**

1. **Commit & Push:**
   - Commit all work in progress
   - Push to remote repository
   - Even if incomplete - mark as WIP

2. **Update PROGRESS.md:**
   - Current status of all tasks
   - What was just completed
   - What's next
   - Any blockers

3. **Update SUB_AGENTS.md:**
   - Status of all sub-agents
   - Last known activity
   - Assigned tasks

4. **Document Context:**
   - Any important decisions made
   - Current train of thought
   - Challenges encountered
   - Ideas for next steps

### After Interruption (Resume Session)

**Always do these when resuming:**

1. **Read PROGRESS.md:**
   - Understand current state
   - Identify what was in progress
   - Check for blockers

2. **Read SUB_AGENTS.md:**
   - Check status of sub-agents
   - Determine if need to spawn new agents
   - Continue or re-assign tasks

3. **Pull Latest Code:**
   - `git pull origin develop`
   - Check for conflicts
   - Review recent commits

4. **Review DECISIONS.md:**
   - Understand decisions made
   - Continue with same approach

5. **Resume or Adjust:**
   - Continue from where left off
   - Or adjust plan based on new information
   - Update PROGRESS.md with resumption

---

## 📝 DOCUMENTATION STANDARDS

### Code Documentation

**Comments:**
- Explain WHY, not WHAT
- Document complex logic
- Document security considerations
- Document assumptions

**Function/Method Docs:**
- Purpose
- Parameters (types, constraints)
- Return value (type, meaning)
- Exceptions/errors
- Examples (for complex functions)

**README files:**
- Every component has README
- Setup instructions
- Configuration
- Testing
- Deployment

### API Documentation

**Use OpenAPI/Swagger:**
- Document all endpoints
- Request/response schemas
- Authentication requirements
- Error responses
- Examples

**Generate automatically:**
- FastAPI auto-generates docs
- Keep schemas updated
- Add descriptions

---

## 🎯 SUCCESS CRITERIA

### MVP Success

**Technical:**
- [ ] All core features working
- [ ] Both encryption tiers functional
- [ ] All platforms deployed
- [ ] 80%+ test coverage
- [ ] Security requirements met
- [ ] Performance targets met
- [ ] Documentation complete

**User Experience:**
- [ ] Users can sign up (both tiers)
- [ ] Users can create encrypted entries
- [ ] Users can upload media
- [ ] Users can search (tier-appropriate)
- [ ] Users can push/pull from Facebook
- [ ] Multi-device sync works
- [ ] Offline mode works (mobile)

**Infrastructure:**
- [ ] Backend deployed and stable
- [ ] iOS in TestFlight
- [ ] Android in Play Console internal testing
- [ ] Web deployed and PWA installable
- [ ] Monitoring in place
- [ ] Wiki complete

### Production Readiness

- [ ] Security audit passed
- [ ] Performance testing passed
- [ ] Beta testing completed
- [ ] User feedback incorporated
- [ ] App Store submissions ready
- [ ] Privacy policy published
- [ ] Terms of service published
- [ ] Support channels established

---

## 🔄 CONTINUOUS IMPROVEMENT

### Code Review

**Review your own code:**
- Read through before committing
- Look for improvements
- Check for security issues
- Ensure tests pass
- Verify documentation

**Review sub-agent code:**
- Check for consistency
- Verify quality standards
- Ensure security practices
- Provide constructive feedback

### Refactoring

**When to refactor:**
- Code duplication found
- Complex logic hard to understand
- Performance bottlenecks
- Better patterns identified

**How to refactor:**
- Ensure tests pass first
- Refactor incrementally
- Keep tests passing
- Update documentation
- Commit frequently

### Learning

**Stay updated:**
- Security best practices
- Framework updates
- New tools/libraries
- Performance optimization techniques

**Apply learnings:**
- Improve code quality
- Enhance security
- Optimize performance
- Better user experience

---

## 💡 PROBLEM-SOLVING APPROACH

### When Stuck

**Process:**

1. **Define the problem clearly**
   - What exactly isn't working?
   - What is the expected behavior?
   - What is the actual behavior?

2. **Gather information**
   - Error messages
   - Logs
   - Stack traces
   - Reproduction steps

3. **Research**
   - Check documentation
   - Search for similar issues
   - Review specification
   - Check code examples

4. **Hypothesize**
   - What could be causing this?
   - List possible causes
   - Prioritize by likelihood

5. **Test hypotheses**
   - Isolate the problem
   - Test one thing at a time
   - Verify each change

6. **Document solution**
   - What was the problem?
   - What was the solution?
   - How to prevent in future?
   - Update DECISIONS.md if relevant

### When to Ask for Clarification

**Don't hesitate to note:**
- Specification ambiguities
- Contradictory requirements
- Impossible constraints
- Missing information

**Document in PROGRESS.md:**
- Question/clarification needed
- Why it's important
- Impact on timeline
- Proposed solution (if any)

**But continue with best judgment:**
- Make reasonable assumptions
- Document assumptions
- Proceed with implementation
- Can adjust later if needed

---

## 🎓 LEARNING RESOURCES

### Official Documentation

**Always refer to:**
- Technology documentation (FastAPI, SwiftUI, Jetpack Compose, React)
- Platform documentation (iOS Human Interface Guidelines, Material Design)
- Security best practices (OWASP)
- Encryption standards (Web Crypto API, CryptoKit)

### Code Examples

**Look for:**
- Official sample projects
- Well-reviewed GitHub repositories
- Tutorial code (verify quality)
- Stack Overflow (verify and adapt)

**Never:**
- Copy-paste without understanding
- Use outdated examples
- Use insecure code
- Ignore licenses

---

## 🎬 FINAL REMINDERS

### You Are Autonomous

- Make decisions confidently
- Choose best implementations
- Don't wait for approval
- Document your choices
- Proceed with development

### Quality Matters

- Time is not a concern
- Build it right the first time
- Don't skip tests
- Don't skip documentation
- Don't compromise security

### Coordinate Effectively

- Use sub-agents for parallelization
- Communicate through PROGRESS.md
- Resolve blockers quickly
- Merge work regularly

### Track Everything

- Update PROGRESS.md daily
- Commit frequently
- Document decisions
- Update wiki continuously

### You Can Do This

- Specifications are comprehensive
- Requirements are clear
- You have all the information needed
- Make it happen

---

## 🚀 BEGIN IMPLEMENTATION

**Your first command:**

```bash
cd /Users/jaystuart/dev/personal-diary
# Read all specifications
# Set up progress tracking
# Make technology decisions
# Initialize git repository
# Spawn sub-agents
# Begin development
```

**Good luck! Build something amazing. 🎉**

---

**End of Initial Agent Instructions**

Everything you need is documented. Now go build the Personal Diary Platform autonomously and excellently.
