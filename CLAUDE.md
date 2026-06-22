# Claude Code Instructions for Hestia Backend

> **Working Agreement — read before starting any task:**
>
> 1. **Tests are part of the task.** Before marking any task complete, write the corresponding tests following `.cursor/rules/03_backend-testing-strategy-local.mdc`. Unit tests (JUnit 5 + Mockito) for domain logic; integration tests (Testcontainers + Spring GraphQL) for API + persistence. Tests must pass before the task is considered done.
> 2. **Domain documentation is part of the task.** When making changes to a domain:
>    - **Read** `src/main/java/com/hestia/[domain]/README.md` first to understand context
>    - **Update** the README with changes made (what, why, use cases)
>    - **Document** database migrations, GraphQL API changes, and architecture decisions
>    - Domain READMEs provide cross-session context and institutional knowledge
> 3. **Continuous doc improvement — with approval.** While working on a task, if you discover a concept, pattern, or strategy (in development, testing, or requirements) that could improve project quality:
>    - **During Plan Mode:** Include governance update proposals in the plan file under a "Governance Updates" section
>    - **Before ExitPlanMode:** Explicitly ask: "Should I add [specific learning] to [specific .mdc file]?" using the AskUserQuestion tool
>    - **Never proceed without explicit "yes"** - silence or "looks good" about the plan does NOT count as approval for governance changes
>    - **After user approval:** Add governance updates in the same session/PR as the feature implementation (keeps learning and code together)
>    - The `.cursor/rules/` directory is the correct location for `.mdc` governance files.
> 4. **Proactive code review before commits.** Before creating any commit:
>    - **Claude proactively runs code review** by invoking Task tool with feature-dev:code-reviewer agent
>    - **Review scope:** All staged changes (or all uncommitted changes if nothing staged)
>    - **Review focus:** Bugs, logic errors, security issues, governance compliance, test coverage
>    - **If issues found:** Report issues grouped by severity, then ask user: "Fix these issues using Ralph Loop?"
>    - **If no issues or after fixes:** Proceed with commit creation
>    - **Implementation:** Claude invokes `Task(subagent_type: "feature-dev:code-reviewer")` before creating commits
>    - **User experience:** Review happens proactively as part of commit workflow, user doesn't need to request it
> 5. **Pre-pipeline verification is mandatory.** Before triggering any CI/CD pipeline (`gh workflow run`):
>    - **Compile check:** `mvn clean compile` must pass
>    - **Spotless check:** `mvn spotless:check` must pass (if it fails locally due to JDK mismatch, manually verify the flagged formatting)
>    - **Unit tests:** `mvn test` must pass
>    - **Never trigger a pipeline with code that hasn't been locally verified.** CI build minutes are expensive and deploy failures block the team.

This is the **backend repository** of the Hestia project - a cross-platform event RSVP application.

**Repository Context:**

- **This repo (backend):** `/Users/vasu/workspaces/hestia/hestia-backend`
- **Frontend repo:** `/Users/vasu/workspaces/hestia/hestia-frontend`

---

## Autonomous Commands (No Permission Required)

Claude can run the following commands **autonomously without asking for permission** when working on backend tasks:

### Build & Compilation
```bash
mvn clean compile                    # Compile source code
mvn clean install                    # Build and install to local Maven repo
mvn package                          # Package as JAR
mvn clean                            # Clean target directory
```

### Testing
```bash
mvn test                             # Run all unit tests
mvn test -Dtest=ClassName            # Run specific test class
mvn test -Dtest=ArchitectureTest     # Run architecture tests
mvn -P it verify                     # Run integration tests
mvn verify                           # Run all tests (unit + integration)
```

### Database Migrations
```bash
mvn flyway:info                      # Show migration status
mvn flyway:migrate                   # Apply pending migrations
mvn flyway:validate                  # Validate migrations
```

### Development Server
```bash
mvn spring-boot:run                  # Start Spring Boot application (local profile)
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Code Quality & Formatting
```bash
mvn spotless:check                   # Check code formatting
mvn spotless:apply                   # Auto-format code
mvn checkstyle:check                 # Run Checkstyle
mvn spotbugs:check                   # Run SpotBugs
```

### File Operations
```bash
ls                                   # List files and directories
ls -la                               # List with details
find . -name "*.java"                # Find Java files
grep -r "pattern" src/               # Search in source files
cat file.txt                         # Read file contents
head -n 20 file.txt                  # Read first 20 lines
tail -n 20 file.txt                  # Read last 20 lines
cp source.txt dest.txt               # Copy files
cp -r src/ dest/                     # Copy directories recursively
```

### Navigation
```bash
cd /path/to/directory                # Change directory
pwd                                  # Print working directory
```

### Git Operations (Read-Only)
```bash
git status                           # Show git status
git diff                             # Show unstaged changes
git diff --staged                    # Show staged changes
git log                              # Show commit history
git log --oneline                    # Show compact commit history
git branch                           # List branches
git branch -vv                       # List branches with tracking info
git worktree list                    # List all worktrees
```

### Project Information
```bash
mvn dependency:tree                  # Show dependency tree
mvn help:effective-pom               # Show effective POM
mvn versions:display-dependency-updates  # Show outdated dependencies
```

**NOTE**: Destructive operations (git push, git reset --hard, mvn flyway:clean, file deletions) always require explicit user permission.

---

## Rule Priority System (CRITICAL)

**When rules conflict, follow this priority order (top = highest priority):**

1. **CLAUDE.md** (this file) - Takes precedence over all .cursor/rules/*.mdc files
2. **.cursor/rules/*.mdc files** - Rules at the top of each file take precedence over rules at the bottom
3. **New rules are always added at the top** of .mdc files to ensure highest priority

**Example conflict resolution:**
- If CLAUDE.md says "use JUnit 5" and 03_backend-testing-strategy-local.mdc says "use TestNG", follow CLAUDE.md (JUnit 5)
- If a rule at line 10 of an .mdc file conflicts with a rule at line 100, follow the rule at line 10
- When adding a new pattern/rule, add it to the top of the relevant .mdc file

**Why this matters:**
- Prevents ambiguity when rules evolve
- Ensures latest patterns take precedence
- Makes conflict resolution deterministic

---

## NON-NEGOTIABLE: Backend is the Single Source of All Logic

**ALL business logic MUST live in the backend.** The frontend (web, iOS, Android) is a presentation layer only — it receives data, displays it, collects input, and sends it back. It does NOT make business decisions.

This means:
- **Validation rules**: Backend enforces all constraints. Frontend validation is UX-only (never authoritative).
- **Date/time/timezone logic**: Backend computes and stores correct values. Frontend displays what it receives.
- **Authorization**: Backend checks permissions. Frontend hides/shows UI based on backend responses, never decides access.
- **Feature gating**: Backend controls feature availability. Frontend renders based on backend flags.
- **Data transformations**: Backend computes derived values. Frontend receives ready-to-display data.

**Why:** One backend serves web + iOS + Android. Logic in the frontend gets duplicated, drifts across platforms, and can be bypassed. Logic in the backend is implemented once, tested once, enforced everywhere.

**Implication for new features:** When designing a feature, ask "where does this decision happen?" — the answer should always be "the backend." If you find yourself adding logic to the frontend, move it to the backend and expose the result via GraphQL.

---

## Git Hooks & Post-Commit Workflow (MANDATORY)

**This repository uses Git hooks to enforce quality gates and provide task reminders. Claude MUST honor these hooks.**

### Hook Setup

Git hooks are located in `.githooks/` and automatically active via `git config core.hooksPath .githooks`. No manual setup needed for new clones.

### Pre-Commit Hook (SELF-GOVERNANCE CHECK)

**Automatically runs before every commit:**
- 📋 **Governance documentation reminder** (from `.githooks/pre-commit-governance-check`)
- ⚠️ **NOTE:** Spotless formatting check currently disabled due to Java 25 compatibility

**What the pre-commit hook asks:**

```
🔍 Governance & Documentation Check:

📋 Governance Updates (.cursor/rules/*.mdc):
   - Have you identified learnings that should become patterns?
   - Have you documented new conventions or architecture decisions?

💡 Note: Domain README updates are handled in post-commit hook.
```

**Claude's MANDATORY actions before committing:**

1. **Review your changes for learnings:**
   - Did you discover a new pattern that should be documented?
   - Did you make an architecture decision that affects future work?
   - Did you solve a tricky problem in a way others should know about?

2. **If YES to any of the above:**
   - **STOP** - Do not proceed with commit yet
   - **ASK** user: "I found [specific learning]. Should I add this to [specific .mdc file] before committing?"
   - **WAIT** for explicit "yes" approval
   - **UPDATE** the governance file if approved
   - **THEN** commit (governance update included in same commit)

3. **If NO learnings:**
   - Proceed with commit
   - The reminder has done its job

**Why this matters:**
- Governance documents evolve organically from real development work
- Patterns are captured when they're fresh in mind
- Future Claude sessions benefit from documented learnings
- Self-governing means Claude takes responsibility for identifying when rules should be added

**Example scenario:**
```
Claude: "I discovered that when adding JPA relationships, setting the
bidirectional link before saving prevents foreign key errors. Should I
add this to 03_backend-testing-strategy-local.mdc under 'JPA Patterns'?"

User: "Yes, that's a good pattern"

Claude: *Updates governance file, then commits with both the code fix
and the governance update in the same commit*
```

### Post-Commit Hook (CRITICAL - CLAUDE MUST FOLLOW)

**After EVERY commit, the post-commit hook displays a task checklist. Claude MUST complete ALL tasks before considering work done:**

#### 1. 📝 Update Linear Ticket (MANDATORY)
- **What:** Add a comment to the relevant Linear ticket (e.g., INV-20)
- **How:** Use Linear MCP `create_comment` tool
- **Include:**
  - Commit hash (short form: `git rev-parse --short HEAD`)
  - Summary of changes
  - Files modified
  - Links to PR if applicable

**Example:**
```
**Backend fixes** (commit `abc1234`)

Addressed production safety concerns from PR review:
- Added environment guard to V008 migration
- Restricted CORS to dev profile only

Files: V008__seed_test_data.sql, CorsConfig.java
PR: https://github.com/user/repo/pull/6
```

#### 2. 🔀 Check PR Mergeability (MANDATORY if PR exists)
- **What:** Verify PR can be merged
- **How:** Run `gh pr view --json mergeable,mergeStateStatus`
- **Action:**
  - If `MERGEABLE`: ✅ Note in response
  - If conflicts: Resolve them immediately
  - If review comments: Address them

#### 3. 🧪 Verify Tests (MANDATORY)
- **What:** Ensure tests pass for the commit
- **How:** Run `mvn test` (unit) and optionally `mvn -P it verify` (integration)
- **Action:**
  - If tests fail due to your changes: Fix immediately
  - If tests fail due to pre-existing issues: Document which failures are pre-existing vs new
  - Never leave broken tests without explaining the cause

#### 4. 📚 Domain README Updates (MANDATORY if domain code changed)
- **What:** Update domain README.md files with changes
- **Where:**
  - `hestia-user/src/main/java/com/hestia/user/README.md`
  - `hestia-event/src/main/java/com/hestia/event/README.md`
  - `hestia-invitation/src/main/java/com/hestia/invitation/README.md`
- **Include:**
  - Domain changes (what/why)
  - Database schema changes (migrations, tables, indexes)
  - GraphQL API changes (new queries/mutations)
  - Architecture decisions
  - Testing coverage

**IMPORTANT:** The post-commit hook output is NOT optional. Claude must explicitly address each task in the conversation, showing what was done or why it was skipped.

### Enforcement

**Why this matters:**
- Linear tickets stay updated with progress (important for team coordination)
- PRs don't get stuck in unmergeable states
- Test failures are caught and addressed immediately
- Domain documentation stays current across sessions

**How Claude should handle it:**
1. Commit is made → post-commit hook displays checklist
2. Claude **immediately** works through each task in order
3. Claude **explicitly reports** completion of each task to the user
4. Only after all tasks are complete should Claude consider the work done

**If Claude skips these tasks:**
- User has to manually do them (defeats the purpose of automation)
- Context is lost for future sessions
- PRs become harder to review and merge

---

## Governance Documents

This project has **10 comprehensive governance documents** that define all development standards, patterns, and conventions. These are located in `.cursor/rules/` and must be followed for all development work.

**IMPORTANT:** Before making significant changes, consult the relevant governance documents below:

1. **01_decision-making.mdc** - When to ask the user vs. making autonomous decisions
2. **02_backend_architecutre_stack.mdc** - Backend architecture, AWS platform (cost-optimized), DDD + JPMS patterns
3. **03_backend-testing-strategy-local.mdc** - Testing requirements (unit, integration, mutation testing)
4. **04_backend_deployment_architecute.mdc** - AWS deployment architecture (shared VPC, $107/month)
5. **05_graphql_api_conventions.mdc** - GraphQL schema design, error handling, pagination, validation
6. **06_database_migrations.mdc** - Flyway migration patterns, safe schema changes, rollback strategies
7. **07_java_spring_boot_conventions.mdc** - Java/Spring Boot coding conventions
8. **08_code_quality_workflow.mdc** - Code quality workflow and tools
9. **09_rate-limiting-strategy.mdc** - Rate limiting strategy (WAF + application), per-endpoint limits, IP hashing, adding new rate-limited endpoints
10. **10_ai-implementation-governance.mdc** - AI-assisted implementation governance: search-before-create, no duplicate logic, architecture boundary preservation, security review, meaningful tests

---

## Domain Documentation (Cross-Session Context)

Each domain folder contains a **README.md** file that serves as living documentation and cross-session memory. These READMEs are critical for understanding domain history, requirements, and implementation decisions.

**When working on a domain, ALWAYS:**

1. **Read the domain README first** - `src/main/java/com/hestia/[domain]/README.md`
   - Understand requirements, use cases, and architecture decisions
   - Review GraphQL API endpoints and error codes
   - Review database schema, migrations, and indexes
   - Check integration test coverage and TODOs

2. **Update the domain README when making changes:**
   - Document WHAT changed (files, components, API changes, schema changes)
   - Document WHY changes were made (rationale, problem solved)
   - Document use cases supported or added
   - Add database migrations to migration history section
   - Add GraphQL API changes (new queries/mutations/errors)
   - Update architecture decisions with ADR format
   - Add to "Bug Fixes & Improvements" section with commit references

3. **Domain READMEs provide:**
   - Requirements and business rules
   - GraphQL API documentation (queries, mutations, errors)
   - Database schema with indexes and constraints
   - Migration history with rationale and rollback strategies
   - Domain model (entities, value objects, aggregates)
   - Architecture decisions (ADRs)
   - File structure and responsibilities
   - Validation rules
   - Bug fixes with root cause analysis
   - Test coverage status
   - Historical context for future sessions

**Example Domain READMEs:**

- `src/main/java/com/hestia/user/README.md` - User domain (authentication, profile)
- `src/main/java/com/hestia/event/README.md` - Event domain (creation, management)
- `src/main/java/com/hestia/invitation/README.md` - Invitation domain (send, RSVP)

**Why this matters:**

- Enables cross-session context without digging through git history
- Documents "why" decisions, not just "what" code
- Helps future Claude sessions understand domain evolution
- Creates institutional knowledge for the project
- Documents database schema evolution with migrations

**Pre-commit reminder:** The `.githooks/pre-commit-governance-check` hook reminds you to update domain READMEs before committing.

---

## Critical Rules Summary

### Technology Stack (LOCKED - Do Not Change)

**Core Stack:**

- Java 21
- Spring Boot 3.x (latest stable)
- Spring GraphQL (schema-first design)
- PostgreSQL 16.x
- Flyway (database migrations)
- Maven (build tool)
- JUnit 5 (unit testing)
- Mockito Classic (mocking)
- Testcontainers (integration testing with PostgreSQL)
- Docker (containerization)
- AWS (deployment: EC2, RDS, ALB, CloudFront)
- Terraform (infrastructure as code)

**Module Organization (DDD + JPMS — 7 Maven modules):**

```
hestia-backend/
├── hestia-shared/             # Shared kernel (value objects, Email, UserId)
├── hestia-notification/       # Notification platform (EmailSender, NotificationService, push)
├── hestia-user/               # User domain (auth, registration, profile, password reset)
├── hestia-event/              # Event domain (creation, update, image upload)
├── hestia-invitation/         # Invitation domain (send, RSVP, reminders)
└── hestia-app/                # Spring Boot application (wiring, listeners, config)
```

**Reactor build order:** Shared → Notification → User → Event → Invitation → App

Each module follows the same internal structure:
```
com.hestia.[domain]/
├── module-info.java           # JPMS module descriptor
├── domain/                    # Domain layer (entities, value objects)
├── application/               # Application layer (services, commands)
├── infrastructure/            # Infrastructure layer (JPA, repositories)
│   ├── persistence/           # JPA entities, repository adapters
│   └── graphql/               # GraphQL resolvers
└── README.md                  # Domain documentation
```

**Database Migrations:**

```
src/main/resources/db/
├── migration/                 # Schema migrations (V001, V002, ...)
└── seed/                      # Dev seed data (V900, V901, ...)
```

**GraphQL Schema:**

```
src/main/resources/graphql/
├── schema.graphqls            # Root schema (Query, Mutation)
├── types/                     # Domain types (user.graphqls, event.graphqls)
├── inputs/                    # Input types
└── common/                    # Common types (pagination, errors, scalars)
```

### Development Philosophy

1. **Domain-Driven Design (DDD)** - Organize code by business domain, not technical layers
2. **Explicit Over Implicit** - Predictable, readable code over abstractions
3. **Test-Driven Development** - Unit tests first, integration tests second
4. **Schema-First GraphQL** - Write GraphQL schema before implementing resolvers
5. **Safe Database Migrations** - Immutable, sequential, reversible migrations
6. **Cost-Conscious** - Optimize for minimal AWS costs (~$107/month target)
7. **Ask, Don't Guess** - When there's ambiguity, ask the user instead of guessing

### Code Patterns (Non-Negotiable)

**Java:**

- Always use Java 21 features (records, pattern matching, sealed classes)
- Use JPMS (Java Platform Module System) for module boundaries
- Follow DDD tactical patterns (entities, value objects, aggregates)
- Package-by-feature, not package-by-layer

**GraphQL:**

- **API backward compatibility is NON-NEGOTIABLE** — every schema change must be backward-compatible with released mobile binaries. See `.cursor/rules/05_graphql_api_conventions.mdc` Section 1 for the full protocol.
- Schema-first design (write `.graphqls` files first)
- Use Input objects for mutations (not individual arguments)
- Return Payload types from mutations (not entities directly)
- Implement cursor-based pagination for unbounded lists
- Use typed errors with `ErrorCode` enum

**Database:**

- Flyway migrations: `V{version}__{description}.sql` (e.g., `V001__create_users_table.sql`)
- Idempotent patterns: `IF NOT EXISTS`, `IF EXISTS`, `ON CONFLICT DO NOTHING`
- Never modify committed migrations
- Document rollback strategy in every migration
- Add nullable columns first, backfill data, then add NOT NULL constraint

**Testing:**

- Unit tests: JUnit 5, no Spring context, mock external edges only
- Integration tests: Testcontainers PostgreSQL, Spring GraphQL test client
- Mutation testing: PIT (target >80% mutation score)
- Test behavior, not implementation
- **MANDATORY: Any GraphQL API change requires an integration test before marking task complete**

**Architecture:**

- Hexagonal Architecture (Ports & Adapters)
- Domain layer has NO dependencies on infrastructure
- Application layer orchestrates use cases
- Infrastructure layer implements ports (JPA repositories, GraphQL resolvers)

### Notification Architecture (INV-72)

The project uses a **Hexagonal Notification Platform** (`hestia-notification` module) that decouples business logic from delivery channels. All notification-sending code MUST go through `NotificationService` — never call `EmailSender` directly from business logic.

**Call chain:**
```
Business Logic (services, listeners)
  └─ NotificationService.send(NotificationRequest)
       └─ NotificationServiceImpl (persists to DB, dispatches channels)
            ├─ EmailChannelAdapter.send(email, type, data)
            │    └─ EmailChannelAdapterImpl → EmailSender (SES/Local/SQS)
            └─ ExpoPushChannelAdapter.send(userId, type, data)
                 └─ Expo Push API
```

**NotificationRequest API:**
```java
// Email-only (OTP, verification, password reset)
NotificationRequest.emailOnly(recipientEmail, recipientUserId, type, data)

// Email + Push (invitations, reminders, RSVP responses)
NotificationRequest.emailAndPush(recipientEmail, recipientUserId, type, data)
```

**NotificationType enum values:**
`RSVP_RESPONSE`, `INVITATION`, `REMINDER`, `EVENT_CANCELLED`, `VERIFICATION`, `PASSWORD_RESET`, `ACCOUNT_DELETION_OTP`, `ACCOUNT_DELETION`

**OTP types** (`VERIFICATION`, `PASSWORD_RESET`, `ACCOUNT_DELETION_OTP`) are enforced as EMAIL-only — the `NotificationServiceImpl` strips non-EMAIL channels for these types.

**Data is passed as `Map<String, String>`** — since `Map.of()` doesn't accept null values, use `""` as a null sentinel. The `EmailChannelAdapterImpl` has an `emptyToNull()` helper to restore null semantics for nullable fields (e.g., `dietaryRestrictions`, `guestMessage`).

**Adding a new notification type:**
1. Add value to `NotificationType` enum
2. Add case to `EmailChannelAdapterImpl.send()` switch statement
3. Add email template to `SesEmailSender` and `LocalEmailSender`
4. Call `notificationService.send(NotificationRequest.emailOnly/emailAndPush(...))` from business logic
5. Update unit tests: mock `NotificationService`, verify with `argThat((NotificationRequest req) -> req.type() == NotificationType.NEW_TYPE)`

**Test pattern (unit tests):**
```java
// Mock NotificationService, NOT EmailSender
@Mock private NotificationService notificationService;

// Verify notification type and key data fields
verify(notificationService).send(argThat(
    (NotificationRequest req) ->
        req.recipientEmail().equals("alice@test.com")
            && req.type() == NotificationType.INVITATION
            && req.data().get("guestName").equals("Alice")));

// Verify no notification sent
verify(notificationService, never()).send(any());

// Verify notification doesn't fail the operation
doThrow(new RuntimeException("SES unavailable"))
    .when(notificationService).send(any(NotificationRequest.class));
```

**Files that legitimately import `EmailSender`** (infrastructure layer only):
- `EmailSender.java` — interface definition
- `LocalEmailSender.java`, `SesEmailSender.java`, `SqsEmailPublisher.java` — implementations
- `EmailChannelAdapterImpl.java` — adapter bridge (sole caller from NotificationService path)
- `SqsEmailConsumer.java` — SQS queue consumer
- `EmailMessage.java` — SQS message data types
- `DevVerificationController.java` — dev-only OTP retrieval for E2E tests

**Push notification feature flag:**
- `hestia.notification.push.enabled=false` (base default, safe for local dev)
- `true` in `application-dev.yml` and `application-prod.yml` (deployed environments)
- When `true`: `ExpoPushChannelAdapter` activates via `@ConditionalOnProperty`
- When `false`: `StubPushChannelAdapter` activates via `@ConditionalOnMissingBean` (no-op)
- IT tests inherit base default (`false`) — no real HTTP calls to Expo during tests

---

### Decision-Making Rules

**When to ask the user:**

- Ambiguous requirements or multiple valid approaches
- Architectural decisions that affect multiple domains
- Changes to locked technology stack
- Breaking changes to GraphQL schema
- Database migrations that risk data loss
- Security or authentication patterns

**When to proceed autonomously:**

- Bug fixes with clear root cause
- Adding tests for existing code
- Refactoring that doesn't change behavior
- Documentation updates (governance, domain READMEs)
- Code formatting and linting fixes
- Database migrations following safe patterns

---

## Common Operations

### Git Worktree & Branch Cleanup (NON-NEGOTIABLE)

> **ALL changes MUST be done in a git worktree on a feature branch — NEVER commit directly to `main`.**
> After a PR is merged, the worktree and local branch MUST be deleted immediately.

**Purpose:** `main` is protected. All changes go through PRs from feature branches in worktrees.

#### Workflow for New Linear Tickets

**Step 1: Before creating a new branch for a Linear ticket**

Check for merged/stale branches and worktrees:

```bash
# List all worktrees
git worktree list

# List all branches with merge status
git branch -vv
```

**Step 2: Clean up merged branches and worktrees**

```bash
# Option A: Automated cleanup (recommended)
# Uses commit-commands:clean_gone skill to remove all [gone] branches
/clean_gone

# Option B: Manual cleanup
# Remove worktree
git worktree remove .claude/worktrees/[old-branch-name]

# Delete local branch (only if merged)
git branch -d [old-branch-name]

# Delete remote branch (if not auto-deleted by GitHub)
git push origin --delete [old-branch-name]
```

**Step 3: Create new branch for the ticket**

```bash
# From main repo or a reusable worktree
git checkout main
git pull origin main
git checkout -b claude/inv-XX-feature-name

# OR create new worktree if needed for parallel work
git worktree add .claude/worktrees/inv-XX-feature-name -b claude/inv-XX-feature-name
cd .claude/worktrees/inv-XX-feature-name
mvn clean install  # Install dependencies (backend equivalent of npm install)
```

#### Worktree Strategy

**Recommended approach:**

1. **Keep 1-2 reusable worktrees** for sequential development:
   - Example: `.claude/worktrees/festive-hertz` or `.claude/worktrees/dev`
   - Reuse by switching branches within the worktree
   - Saves disk space, faster setup (no repeated `mvn clean install`)

2. **Create new worktrees only when needed:**
   - When working on 2+ tickets simultaneously
   - When main worktree has uncommitted work
   - For long-running feature branches

3. **Clean up after PR is merged:**
   - Delete the branch (local and remote)
   - Remove the worktree if it was ticket-specific
   - Keep reusable worktrees, just switch branches

#### Exception: Main Worktree

The main repository checkout (not in `.claude/worktrees/`) should:
- Stay on `main` or a stable branch
- Not be used for active development (prefer worktrees)
- Be used for administrative tasks (releases, merges)

#### Automation

**Git hook integration:**

Git hooks are configured via `.githooks/` directory (tracked in Git).

To enable hooks after cloning:

```bash
.githooks/setup.sh
```

This configures Git to use `.githooks/` for all worktrees and branches.

**Enforcement:**

- ✅ Claude will check for merged branches before creating new ones
- ✅ Claude will offer to run `/clean_gone` before starting new tickets
- ✅ Prevents accumulation of stale worktrees

#### Benefits

- **Clean git state** - Only active branches visible
- **Disk space savings** - No duplicate Maven dependencies in old worktrees
- **Mental clarity** - Fewer directories to navigate
- **Faster setup** - Reusing worktrees avoids repeated Maven builds

**See**: `.cursor/rules/01_decision-making.mdc`, Section 1.1 for full details.

---

### Database Migrations

```bash
# Run migrations locally (Flyway applies automatically on Spring Boot startup)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Or run Flyway directly
mvn flyway:migrate

# Clean database (dev only)
mvn flyway:clean flyway:migrate
```

### Testing

```bash
# Unit tests
mvn test

# Integration tests
mvn -P it verify

# Mutation testing
mvn org.pitest:pitest-maven:mutationCoverage

# All tests
mvn verify
```

### Development

```bash
# Start Spring Boot app (local profile)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Build Docker image
mvn spring-boot:build-image

# Run with Docker
docker run -p 8080:8080 hestia-backend:latest
```

### AWS / Deployment

```bash
# Set AWS profile for Terraform and deployment commands
export AWS_PROFILE=hestia-bot

# Bootstrap Terraform backend (one-time)
./scripts/bootstrap-terraform-backend.sh

# Run Terraform
cd terraform/cicd
terraform init
terraform plan -var="github_org=YOUR_GITHUB_USERNAME"
terraform apply -var="github_org=YOUR_GITHUB_USERNAME"
```

See [devloop/setup.md](devloop/setup.md) for full AWS setup instructions.

### Code Quality

```bash
# Checkstyle
mvn checkstyle:check

# SpotBugs
mvn spotbugs:check

# Format code
mvn spotless:apply

# All validations
mvn verify
```

---

## Initial Java/Spring Boot Conventions (MANDATORY)

These conventions apply from day 1 of backend development. Follow these patterns unless explicitly documented otherwise.

### 1. Package Structure (DDD + JPMS)

**Pattern:**
```
com.hestia.[domain]/
├── module-info.java           # JPMS module descriptor
├── domain/                    # Domain layer (NO framework dependencies)
│   ├── [Entity].java          # Entities (e.g., User, Event)
│   ├── [ValueObject].java     # Value objects (e.g., Email, Address)
│   ├── [Repository].java      # Repository interfaces (ports)
│   └── [DomainService].java   # Domain services (optional)
├── application/               # Application layer (orchestration)
│   ├── [UseCase]Service.java  # Application services
│   └── commands/              # Command objects (optional)
│       └── [Command].java
├── infrastructure/            # Infrastructure layer (adapters)
│   ├── persistence/
│   │   ├── [Entity]JpaEntity.java  # JPA entities
│   │   └── Jpa[Repository].java    # JPA repository implementations
│   └── graphql/
│       └── [Entity]Resolver.java   # GraphQL resolvers
└── README.md                  # Domain documentation
```

**Example (User domain):**
```
com.hestia.user/
├── module-info.java
├── domain/
│   ├── User.java              # Entity
│   ├── Email.java             # Value object
│   └── UserRepository.java    # Repository interface
├── application/
│   └── UserService.java       # Application service
├── infrastructure/
│   ├── persistence/
│   │   ├── UserJpaEntity.java # JPA entity
│   │   └── JpaUserRepository.java  # JPA implementation
│   └── graphql/
│       └── UserResolver.java  # GraphQL resolver
└── README.md
```

### 2. Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Entity (Domain) | `[Entity]` | `User`, `Event`, `Invitation` |
| Value Object | `[ValueObject]` | `Email`, `Address`, `EventDate` |
| Repository Interface | `[Entity]Repository` | `UserRepository`, `EventRepository` |
| JPA Entity | `[Entity]JpaEntity` | `UserJpaEntity`, `EventJpaEntity` |
| JPA Repository | `Jpa[Entity]Repository` | `JpaUserRepository`, `JpaEventRepository` |
| Application Service | `[Entity]Service` | `UserService`, `EventService` |
| GraphQL Resolver | `[Entity]Resolver` | `UserResolver`, `EventResolver` |
| GraphQL Input | `[Action][Entity]Input` | `CreateUserInput`, `UpdateEventInput` |
| GraphQL Payload | `[Action][Entity]Payload` | `CreateUserPayload`, `UpdateEventPayload` |

### 3. Domain Layer Rules (NO Framework Dependencies)

**Allowed:**
- Java standard library
- Domain-specific libraries (e.g., `java.time` for dates)
- Custom value objects
- Repository interfaces (ports)

**Forbidden:**
- Spring annotations (`@Service`, `@Component`, etc.)
- JPA annotations (`@Entity`, `@Table`, etc.)
- GraphQL annotations
- Any framework-specific code

**Example (Domain Entity):**
```java
// ✅ GOOD: Pure domain entity
public class User {
    private final UserId id;
    private final Email email;
    private final FullName fullName;
    private final Instant createdAt;

    public User(UserId id, Email email, FullName fullName) {
        this.id = requireNonNull(id);
        this.email = requireNonNull(email);
        this.fullName = requireNonNull(fullName);
        this.createdAt = Instant.now();
    }

    public void changeEmail(Email newEmail) {
        // Domain logic here
        requireNonNull(newEmail);
        // Business rules validation
    }
}

// ❌ BAD: Framework dependencies in domain layer
@Entity  // Wrong: JPA annotation in domain layer
public class User {
    @Id @GeneratedValue  // Wrong: JPA annotations
    private Long id;
}
```

### 4. Value Objects (Mandatory for Domain Concepts)

**Use value objects for:**
- Email addresses
- Phone numbers
- Money/currency
- Addresses
- Dates/times (when business rules apply)
- IDs (typed IDs, not `Long` or `UUID` directly)

**Pattern:**
```java
public record Email(String value) {
    public Email {
        requireNonNull(value, "Email cannot be null");
        if (!value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
```

### 5. Repository Pattern (Ports & Adapters)

**Domain Layer (Port):**
```java
// Interface in domain layer
public interface UserRepository {
    User findById(UserId id);
    List<User> findByEmail(Email email);
    void save(User user);
    void delete(UserId id);
}
```

**Infrastructure Layer (Adapter):**
```java
// Implementation in infrastructure layer
@Repository
public class JpaUserRepository implements UserRepository {
    private final UserJpaRepository jpaRepository;

    @Override
    public User findById(UserId id) {
        return jpaRepository.findById(id.value())
            .map(this::toDomain)
            .orElseThrow(() -> new UserNotFoundException(id));
    }

    private User toDomain(UserJpaEntity entity) {
        // Map JPA entity to domain entity
    }

    private UserJpaEntity toJpa(User user) {
        // Map domain entity to JPA entity
    }
}
```

### 5.1 Single-Field Updates on Aggregate Roots

When updating a single field on an aggregate root (e.g., changing `hostId` on an Event), prefer a targeted `@Modifying @Query` over reconstructing the entire domain object.

```java
// ✅ Preferred — targeted SQL update
@Modifying
@Query("UPDATE EventJpaEntity e SET e.hostId = :newHostId WHERE e.id = :eventId")
int updateHostId(@Param("eventId") String eventId, @Param("newHostId") String newHostId);

// ❌ Avoid — reconstructing 25-parameter object just to change one field
Event updated = new Event(event.id(), newHostId, event.title(), event.description(), ...);
eventRepository.save(updated);
```

**Why:** Full object reconstruction risks constructor validation failures (null checks on optional fields), JPA cascade conflicts (orphan removal on `@OneToOne`), and version/optimistic locking issues. A targeted UPDATE avoids all of these.

**Exception:** When the update affects invariants that span multiple fields, reconstruct via the domain model to enforce validation.

### 6. Application Service Pattern

**Pattern:**
```java
@Service
public class UserService {
    private final UserRepository userRepository;
    // Inject other dependencies (repositories, domain services)

    public CreateUserPayload createUser(CreateUserInput input) {
        // 1. Validate input
        // 2. Check business rules
        // 3. Create domain entity
        // 4. Save via repository
        // 5. Return payload
    }
}
```

### 7. GraphQL Resolver Pattern

**Pattern:**
```java
@Controller
public class UserResolver {
    private final UserService userService;

    @QueryMapping
    public User user(@Argument String id) {
        return userService.findUserById(new UserId(id));
    }

    @MutationMapping
    public CreateUserPayload createUser(@Valid @Argument CreateUserInput input) {
        return userService.createUser(input);
    }
}
```

### 8. Error Handling Pattern

**Domain Exceptions:**
```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UserId id) {
        super("User not found: " + id);
    }
}
```

**GraphQL Error Mapping:**
```java
@ControllerAdvice
public class GraphQLExceptionHandler {
    @GraphQlExceptionHandler
    public GraphQLError handleUserNotFound(UserNotFoundException ex) {
        return GraphQLError.newError()
            .message(ex.getMessage())
            .errorType(ErrorType.NOT_FOUND)
            .build();
    }
}
```

### 9. Testing Patterns

**Unit Test (Domain Logic):**
```java
class UserTest {
    @Test
    void shouldChangeEmail() {
        // given
        var user = new User(new UserId("1"), new Email("old@example.com"), new FullName("John", "Doe"));
        var newEmail = new Email("new@example.com");

        // when
        user.changeEmail(newEmail);

        // then
        assertThat(user.email()).isEqualTo(newEmail);
    }
}
```

**Integration Test (GraphQL + PostgreSQL):**
```java
@SpringBootTest
@Testcontainers
class UserResolverIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void shouldCreateUser() {
        String mutation = """
            mutation CreateUser($input: CreateUserInput!) {
                createUser(input: $input) {
                    success
                    user { id email }
                    errors { message code }
                }
            }
            """;

        graphQlTester.document(mutation)
            .variable("input", Map.of("email", "test@example.com", "firstName", "John", "lastName", "Doe"))
            .execute()
            .path("createUser.success").entity(Boolean.class).isEqualTo(true);
    }
}
```

---

## GraphQL API Changes Require Integration Tests (CRITICAL)

**⚠️ MANDATORY RULE: Any GraphQL API change must have an integration test before marking the task complete.**

### What counts as a GraphQL API change:

- New queries or mutations
- New types or fields
- Changes to input validation
- Error handling changes
- Pagination implementation

### Required test coverage:

1. ✅ GraphQL query/mutation executes successfully
2. ✅ Returns expected data structure
3. ✅ Validates input correctly
4. ✅ Returns typed errors for invalid input
5. ✅ Handles edge cases (not found, duplicates, etc.)

### Before marking ANY GraphQL task complete:

```bash
# 1. Write integration test in src/test/java/.../IT.java
# 2. Run the test
mvn -P it verify

# 3. Verify it passes
# 4. Only then mark task as complete
```

**Skipping integration tests for GraphQL changes = incomplete work.**

---

## Before Making Changes

1. **Check relevant governance document** from `.cursor/rules/`
2. **Read domain README** to understand existing patterns
3. **Ask the user** if requirements are ambiguous
4. **Run tests** after changes: `mvn verify`
5. **Update tests** if behavior changes
6. **Update domain README** with changes made
7. **For GraphQL changes: Write integration test BEFORE marking complete**

---

## Troubleshooting & Learnings

### Spring Boot DevTools Classloader Issues

**Problem:** `IllegalAccessError` when using default methods in JPA repository interfaces to access enum types across module boundaries.

**Root Cause:**
- Spring Boot DevTools uses a custom `RestartClassLoader` that loads classes in different modules
- Package-private or inline enum types cannot be accessed by JPA proxy classes
- Interface default methods get proxied, causing module access violations

**Solution:**
- Extract enum types to separate public files (e.g., `InvitationStatusJpa.java`)
- Use adapter pattern instead of interface default methods
- Create a concrete `@Component` adapter class that implements the repository interface
- Keep Spring Data JPA repository as a simple interface without custom default methods

**Example:**
```java
// ❌ BAD: Interface with default methods accessing enums
public interface JpaInvitationRepository extends JpaRepository<Entity, UUID>, InvitationRepository {
    default Invitation save(Invitation inv) {
        // Proxy can't access InvitationStatusJpa enum
        InvitationStatusJpa status = mapStatus(inv.status());
    }
}

// ✅ GOOD: Adapter pattern with concrete class
@Component
public class JpaInvitationRepositoryAdapter implements InvitationRepository {
    private final JpaInvitationRepositorySpring jpaRepository;

    public Invitation save(Invitation inv) {
        // No proxy issues in concrete class
        InvitationStatusJpa status = mapStatus(inv.status());
    }
}

public interface JpaInvitationRepositorySpring extends JpaRepository<Entity, UUID> {
    // No default methods
}
```

### Spring Multi-Constructor DI Requires Explicit `@Autowired` (INV-291 / display-banner-info)

**Problem:** A `@Service` (or other Spring-managed `@Component`) that declares more than one constructor can cause Spring's IT-bootstrap to ambiguously pick the wrong constructor. Unit tests pass; ITs may fail with errors that look like behavior bugs (wrong error code, wrong validation outcome) but are actually wiring-level.

**Root cause:** Spring's `BeanUtils.instantiateClass(...)` constructor-selection algorithm has no disambiguation hint when two candidates exist. The Testcontainers profile may provide a bean (e.g., a `Clock`) that satisfies the test-only constructor, so Spring picks it and the production code path runs with a misconfigured dependency graph.

**Symptom pattern:** Multiple seemingly-unrelated IT failures cluster suddenly after a recent commit that added a second constructor (often a test-only convenience like `BannerServiceImpl(repo, repo, repo, sanitizer, Clock)` next to the production `BannerServiceImpl(repo, repo, repo, sanitizer)`). The failures look like 3 different product bugs (dismiss returns wrong, classification missing, validator not reached) but resolve to a single annotation.

**Rule:** Any `@Service` (or other Spring-managed `@Component`) declaring more than one constructor MUST annotate the autowire-target (production) constructor with `@Autowired`.

```java
// ✅ GOOD — unambiguous
@Service
public class BannerServiceImpl implements BannerService {

    @Autowired
    public BannerServiceImpl(
            BannerRepository bannerRepository,
            BannerDismissalRepository dismissalRepository,
            UserRepository userRepository,
            BannerContentSanitizer sanitizer) {
        this(bannerRepository, dismissalRepository, userRepository, sanitizer, Clock.systemUTC());
    }

    // Package-private — used by unit tests that need a fixed Clock for deterministic assertions
    BannerServiceImpl(
            BannerRepository bannerRepository,
            BannerDismissalRepository dismissalRepository,
            UserRepository userRepository,
            BannerContentSanitizer sanitizer,
            Clock clock) {
        // ...
    }
}

// ❌ BAD — Spring may pick either; IT bootstrap can fail silently or noisily
@Service
public class BannerServiceImpl implements BannerService {

    public BannerServiceImpl(BannerRepository repo, ..., BannerContentSanitizer sanitizer) { ... }

    BannerServiceImpl(BannerRepository repo, ..., BannerContentSanitizer sanitizer, Clock clock) { ... }
}
```

**Diagnostic shortcut:** When a cluster of IT failures lands after a recent commit, before investigating each test individually, `grep -n "public.*(" SomeService.java` to count constructors. Two constructors + no `@Autowired` = the entire cluster is likely one wiring bug.

**Why this isn't auto-detected:** Spring's constructor selection is a runtime decision; the compiler can't warn about it. There's no static-analysis rule in our current Spotless/Checkstyle config to catch this — the rule lives here.

### JPQL Does Not Support PostgreSQL-Specific Functions (e.g., UUID)

**Problem:** Spring Boot fails to start with `UnsatisfiedDependencyException` — query validation fails for `@Query` annotations using `UUID(:param)`.

**Root Cause:**
- JPQL is database-agnostic and has NO `UUID()` function — that's PostgreSQL-specific
- Spring Data validates `@Query` annotations at startup (not at compile time)
- So `mvn clean install -DskipTests` will succeed, but the app will fail to start
- This means you can't catch the error until you actually run the application

**Solution:**
Use `nativeQuery = true` with PostgreSQL SQL syntax:
```java
// ❌ BAD: JPQL — UUID() does not exist in JPQL
@Query("UPDATE UserJpaEntity u SET u.passwordHash = :hash WHERE u.id = UUID(:userId)")
void setPasswordHash(@Param("userId") String userId, @Param("hash") String hash);

// ✅ GOOD: Native query with PostgreSQL CAST
@Modifying
@Query(
    value = "UPDATE users SET password_hash = :hash WHERE id = CAST(:userId AS uuid)",
    nativeQuery = true)
void setPasswordHash(@Param("userId") String userId, @Param("hash") String hash);
```

**Key differences with `nativeQuery = true`:**
- Use physical table names (`users`) not entity names (`UserJpaEntity`)
- Use physical column names (`password_hash`) not field names (`passwordHash`)
- Use PostgreSQL's `CAST(:param AS uuid)` for UUID conversion

**Best Practice:** Always start the backend (`mvn spring-boot:run`) after adding or modifying `@Query` annotations — `mvn test` with mocks won't catch JPQL syntax errors.

### Maven SNAPSHOT Caching Issues

**Problem:** After code changes, backend still uses old cached code even after restart.

**Root Cause:**
- Maven caches SNAPSHOT JARs in `~/.m2/repository/`
- `mvn spring-boot:run` uses cached JARs instead of rebuilding from source
- Changes to dependency modules aren't picked up

**Solution:**
```bash
# Option 1: Force rebuild and update cache
mvn clean install -DskipTests

# Option 2: Delete specific module cache
rm -rf ~/.m2/repository/com/hestia/hestia-invitation/0.1.0-SNAPSHOT/

# Option 3: Force Maven to update snapshots
mvn clean install -U -DskipTests

# Then restart with fresh cache
mvn spring-boot:run -pl hestia-app -Dspring-boot.run.profiles=local
```

**Best Practice:** After significant code changes across modules, always run `mvn clean install` before restarting.

### Stale Compiled Classes After Code Reversion (Multi-Module)

**Problem:** Running `mvn test -pl hestia-app` after reverting code in `hestia-event` (or any dependency module) picks up stale `.class` files from the dependency's `target/` directory. The stale code may reference non-existent database columns or removed methods, causing `InvalidDataAccessResourceUsageException` at runtime — masked by Spring GraphQL as `INTERNAL_ERROR`.

**Root Cause:** `mvn test` without `clean` does not recompile unchanged source files. If a method was added then reverted, the compiled `.class` persists in `target/`. Multi-module builds are especially vulnerable because module A's stale classes are picked up by module B's test classpath.

**Solution:** Always use `mvn clean test` (not `mvn test`) after reverting code changes, especially across modules.

```bash
# After reverting code in any module:
mvn clean test -pl hestia-app -Dtest="AffectedTestClass"

# Or for all tests:
mvn clean verify
```

**Anti-pattern:** `mvn test -pl hestia-app` after `git revert` — stale classes cause runtime failures that look like code bugs but aren't.

### GraphQL DateTime Scalar Mismatch

**Problem:** "Can't serialize value: Expected `java.time.OffsetDateTime` but was `Instant`"

**Root Cause:**
- Domain entities use `java.time.Instant` (UTC timestamps)
- GraphQL default DateTime scalar expects `java.time.OffsetDateTime`

**Solution:**
```java
@Configuration
public class GraphQLScalarConfiguration {
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(instantDateTimeScalar());
    }

    private GraphQLScalarType instantDateTimeScalar() {
        return GraphQLScalarType.newScalar()
            .name("DateTime")
            .coercing(new Coercing<Instant, String>() {
                @Override
                public String serialize(Object result) {
                    if (result instanceof Instant instant) {
                        return DateTimeFormatter.ISO_INSTANT.format(instant);
                    }
                    throw new CoercingSerializeException("Expected Instant");
                }

                @Override
                public Instant parseValue(Object input) {
                    return Instant.parse((String) input);
                }

                @Override
                public Instant parseLiteral(Object input) {
                    if (input instanceof StringValue stringValue) {
                        return Instant.parse(stringValue.getValue());
                    }
                    throw new CoercingParseLiteralException("Expected StringValue");
                }
            })
            .build();
    }
}
```

### JPA Bidirectional Relationship Management

**Problem:** Foreign key constraint violation when saving RSVP with invitation relationship.

**Root Cause:**
- `@OneToOne` bidirectional relationship requires both sides to be set
- The owner side (with `@JoinColumn`) MUST have the reference set for FK to be persisted

**Solution:**
```java
// When saving invitation with new RSVP:
RsvpJpaEntity rsvpEntity = rsvpToJpa(rsvp);
rsvpEntity.setInvitation(entity);  // ✅ CRITICAL: Set owner side!
entity.setRsvp(rsvpEntity);
```

**Rule:** Always set the owning side (@JoinColumn) of bidirectional relationships before saving.

### Integration Test Data Isolation (UUID Prefix Pattern)

**Problem:** Integration tests fail silently due to `ON CONFLICT DO NOTHING` when two test classes share the same UUID-prefixed IDs.

**Root Cause:**
- Each IT test class inserts test data via SQL. If two classes use overlapping UUIDs, the second class's `INSERT ... ON CONFLICT (id) DO NOTHING` silently skips, and tests see stale/wrong data.
- **Email uniqueness is a separate constraint** — `ON CONFLICT (id)` won't catch email collisions. This caused CI-only failures when `UpdateEventResolverIT` and `ImageUploadResolverIT` both used `other-user@example.com`.

**Solution:** Each IT class MUST use a unique UUID prefix AND unique emails:

| Test Class | UUID Prefix | Example Email |
|------------|-------------|---------------|
| EventDetailsResolverIT | `eeeeeeee-*` | `ed-host@example.com` |
| DashboardResolverIT | `dddddddd-*` | `dash-host@example.com` |
| DeleteEventResolverIT | `aabbccdd-*` | `del-host@example.com` |
| CreateEventResolverIT | `cccccccc-*` | `create-host@example.com` |
| PasswordResetIT | `aabb00dd-*` | `pwreset@example.com` |
| ImageUploadResolverIT | `ffff0066-*` | `img-host@example.com` |
| EmailVerificationIT | `vvvvvvvv-*` | `verify@example.com` |

**Rule:** Events MUST have a location row in test data (JPA eager-loads it; missing location = NPE masked as INTERNAL_ERROR).

**Rule:** Operations that touch multiple aggregates (e.g., `transferOwnership` triggers `OwnershipTransferredEvent` → listener updates `invitations` table) must have test data in ALL related tables, not just the primary aggregate. Missing data in secondary tables can mask bugs in BEFORE_COMMIT listeners, since `@Modifying` UPDATE queries silently affect 0 rows.

### Flyway Migration Modification (Local Dev Only)

**Problem:** Flyway fails validation due to checksum mismatch after modifying a migration file that's already been applied.

**Solution (local dev only — NEVER modify deployed migrations):**
```bash
# 1. Remove the migration from flyway history
docker exec -i hestia-postgres-dev psql -U hestia_dev -d hestia_dev \
  -c "DELETE FROM flyway_schema_history WHERE version = 'XX';"

# 2. Drop the affected tables
docker exec -i hestia-postgres-dev psql -U hestia_dev -d hestia_dev \
  -c "DROP TABLE IF EXISTS affected_table CASCADE;"

# 3. Restart backend — Flyway will re-apply the migration
```

### OTP / Email Verification Patterns

- **Configurable TTL pattern:** Backend config → token `expires_at` → email body text → GraphQL response `ttlMinutes` → frontend countdown timer. Backend is always authoritative.
- **Email verification (INV-45):** TTL = 15 min (`hestia.auth.email-verification.token-ttl-minutes`). Rate limit: `max-resends-per-hour=4`.
- **Password reset (INV-54):** TTL = 30 min (`hestia.auth.password-reset.token-ttl-minutes`). Brute-force protection: 5 max attempts/token. Rate limit: 3 requests/hour.
- **Backend config:** `ses.enabled=false` (dev default) → `true` in prod. `DevVerificationController` only loads when `ses.enabled=false`.
- **Local testing:** OTP logged to console via `LocalEmailSender`. No AWS needed for dev.

### Java 25 Module System Prevents Mocking Final JDK Classes (HttpClient)

**Problem:** Mockito cannot mock `java.net.http.HttpClient` or `java.net.http.HttpResponse` on Java 25 due to module system restrictions (`InaccessibleObjectException`).

**Root Cause:**
- Java 25's strong encapsulation blocks reflective access to `java.net.http` internals
- `HttpClient` is a final abstract class — Mockito's inline mock maker needs reflective access to subclass it
- This affects any JDK class in a non-exported package

**Solution:**
Create a thin `@FunctionalInterface` wrapper:
```java
@FunctionalInterface
public interface HttpSender {
    HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException;
}
```

Then inject `HttpSender` instead of `HttpClient`:
- Production: `@Bean` creates `HttpClient` and wraps it as `HttpSender` lambda
- Tests: Mock `HttpSender` directly (it's an interface, fully mockable)

**Example:** See `ExpoPushChannelAdapter` + `PushChannelAdapterConfiguration` + `HttpSender` in `hestia-notification` module (INV-186).

**Best Practice:** When writing new code that calls external HTTP APIs via `java.net.http.HttpClient`, always inject an `HttpSender` (or similar interface) rather than `HttpClient` directly. This ensures testability on Java 25+.

### Java 25 Module System Also Blocks Mocking Third-Party Library Classes

**Problem:** Same Mockito issue as `HttpClient` also applies to concrete classes from third-party libraries like `google-api-client` (`GoogleIdTokenVerifier`, `GoogleIdToken`, `GoogleIdToken.Payload`).

**Root Cause:**
- Java 25's module system prevents Mockito from accessing classes in other modules (not just JDK modules)
- `GoogleIdTokenVerifier` is a concrete class — Mockito needs reflective access to create a subclass proxy
- Error: `Could not modify all classes [class com.google.api.client.googleapis.auth.oauth2.GoogleIdToken...]`

**Solution:** Same `@FunctionalInterface` wrapper pattern as `HttpSender`:
```java
@FunctionalInterface
interface GoogleIdTokenVerifierFunction {
    GoogleTokenClaims verifyAndExtract(String idTokenString)
            throws GeneralSecurityException, IOException;
}
```

The wrapper returns your own domain type (`GoogleTokenClaims`) instead of the library type (`GoogleIdToken`), so the library types don't leak into testable code at all.

**Example:** See `GoogleIdTokenVerifierFunction` + `GoogleOAuthConfiguration` + `GoogleApiClientTokenVerifier` in `hestia-user` module (INV-199).

**Best Practice:** For ANY third-party library class that needs mocking, wrap it behind a functional interface that returns your own domain types. This applies to Google, AWS SDK, Stripe, etc.

### GoogleIdTokenVerifier JWKS Pre-Warm: loadPublicCerts() Not verify()

**Problem:** `verifier.verify("dummy-token")` does NOT pre-warm the JWKS cache — JWT parsing fails at the token structure check before any network call to Google's JWKS endpoint.

**Solution:** Use `verifier.loadPublicCerts()` which directly triggers the JWKS HTTP fetch.

**Timing context:** `google-http-client` `HttpRequest` has 20-second default connect + read timeouts (set in constructor bytecode: `sipush 20000`). The `GooglePublicKeysManager` API does not expose a way to customize these, but 20s is acceptable.

### Spring GraphQL Error Masking

**Problem:** Spring GraphQL masks all unhandled exceptions as `INTERNAL_ERROR` with no useful details.

**Solution:** When debugging, inject the service directly in the IT test and call it without going through GraphQL. This reveals the actual exception stack trace.

---

## Local Deploy (from laptop)

```bash
# Dev deploy (blue-green, ~15-22 min)
./scripts/deploy/local-deploy.sh --environment dev

# Prod deploy (fallback only, requires typed confirmation)
./scripts/deploy/local-deploy.sh --environment prod --confirm-prod
```

**First-time setup:** `cp scripts/deploy/local-deploy.conf.example scripts/deploy/local-deploy.conf` and fill in values from `cd terraform/dev && terraform output`.

**Key files:**
- `scripts/deploy/local-deploy.sh` — main deploy script
- `scripts/deploy/local-deploy.conf.example` — config template
- `scripts/deploy/blue-green-deploy.sh` — blue-green orchestrator (called by local-deploy.sh)
- `scripts/deploy/deploy.sh` — EC2-side deploy script (runs on the instance via SSM)
- `Dockerfile.local` — runtime-only Dockerfile for local builds (avoids Maven under QEMU)

**NON-NEGOTIABLE: Dev Before Prod.** ALL code changes MUST be deployed to DEV AWS first, E2E tested on DEV, and confirmed working BEFORE deploying to PROD. Sequence: `Deploy to DEV → Run full E2E on DEV → Confirm all passing → Only deploy to PROD on explicit user request`.

**NON-NEGOTIABLE: Prod deployment requires explicit user request.** NEVER deploy to prod automatically. After DEV AWS E2E passes, STOP and inform the user. Wait for explicit "deploy to prod" instruction. The user decides when prod is ready, not Claude.

**NON-NEGOTIABLE: Quality over speed.** NEVER skip phases or take shortcuts to speed up delivery. Quality is the priority, speed is secondary. Every phase in `/c_feature` exists for a reason — run all of them, every time.

---

## Remember

- This project prioritizes **domain-driven design, testability, and cost-consciousness**
- Follow the **established patterns** - don't introduce new paradigms without discussion
- **Ask questions** when unsure - it's better than making incorrect assumptions
- **Test your changes** - both unit tests and integration tests
- **Document significant decisions** - update domain READMEs and governance docs as needed
- **Respect the rule priority system** - CLAUDE.md > .cursor/rules/*.mdc (top > bottom)

---

For detailed specifications on any topic, consult the corresponding governance document in `.cursor/rules/`.
