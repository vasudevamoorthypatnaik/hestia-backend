# Contributing to Hestia Backend

Thank you for contributing to the Hestia backend! This guide will help you get started.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Local Development Setup](#local-development-setup)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [Database Migrations](#database-migrations)
- [GraphQL API Development](#graphql-api-development)
- [Code Quality](#code-quality)
- [Git Workflow](#git-workflow)
- [Common Issues](#common-issues)

---

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 21** - Download from [Adoptium](https://adoptium.net/) or use SDKMAN
- **Maven 3.8+** - [Installation guide](https://maven.apache.org/install.html)
- **Docker** - [Installation guide](https://docs.docker.com/get-docker/)
- **Docker Compose** - Usually included with Docker Desktop

### Verify Installation

```bash
java -version   # Should show Java 21
mvn -version    # Should show Maven 3.8+
docker --version
docker-compose --version
```

---

## Local Development Setup

### 1. Clone the Repository

```bash
git clone https://github.com/vasudevamoorthypatnaik/hestia-backend.git
cd hestia-backend
```

### 2. Setup Git Hooks (One-Time)

Configure Git to use project hooks for code quality checks:

```bash
.githooks/setup.sh
```

**What this does:**
- Configures Git to use `.githooks/` directory instead of `.git/hooks/`
- Hooks work in all worktrees and branches automatically
- Runs code formatting check (Spotless) before commits
- Reminds about updating governance docs and domain READMEs

**Hooks enabled:**
- `pre-commit` - Validates code formatting with Spotless
- `pre-commit-governance-check` - Governance reminder (Claude Code only)

### 3. Start PostgreSQL Database

Use Docker Compose to start a local PostgreSQL instance:

```bash
docker-compose up -d
```

This starts PostgreSQL 16 on `localhost:5432` with:
- Database: `hestia_dev`
- Username: `hestia_dev`
- Password: `hestia_dev_password`

Verify PostgreSQL is running:

```bash
docker-compose ps
```

### 4. Build the Project

```bash
mvn clean install
```

This will:
- Download all dependencies
- Compile the source code
- Run unit tests
- Run integration tests (if using `-P it`)
- Create the JAR file in `target/`

### 5. Run Database Migrations

Flyway migrations run automatically when the application starts. To run manually:

```bash
mvn flyway:migrate -Dflyway.configFiles=src/main/resources/application-dev.yml
```

---

## Running the Application

### Option 1: Using Maven (Recommended for Development)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The application will start on **http://localhost:8080**

### Option 2: Using the JAR File

```bash
mvn clean package -DskipTests
java -jar target/hestia-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

### Option 3: Using Your IDE

Run the `HestiaBackendApplication` class with:
- **VM Options**: `-Dspring.profiles.active=local`
- **Environment Variables**: (none required for local dev)

### Access GraphiQL (Development Only)

GraphiQL is enabled in the `local` profile for testing GraphQL queries.

**URL:** http://localhost:8080/graphiql

**Example Query:**

```graphql
query {
  health {
    status
    timestamp
  }
}
```

---

## Running Tests

### Unit Tests Only

```bash
mvn test
```

Unit tests (`*Test.java`) run without Spring context and are fast.

### Integration Tests Only

```bash
mvn -P it verify
```

Integration tests (`*IT.java`) use Testcontainers to spin up a real PostgreSQL database.

### All Tests

```bash
mvn verify
```

### Mutation Testing (PIT)

```bash
mvn -P mutation verify
```

Generates mutation testing report in `target/pit-reports/`.

---

## Database Migrations

### Creating a New Migration

1. Create a new SQL file in `src/main/resources/db/migration/`
2. Follow naming convention: `V{version}__{description}.sql`
   - Example: `V003__add_events_table.sql`

3. Write idempotent SQL:

```sql
-- V003__add_events_table.sql
-- Purpose: Create events table
-- Rollback strategy: DROP TABLE IF EXISTS events;

CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Testing Migrations

Run integration tests to verify migrations:

```bash
mvn -P it verify -Dtest=FlywayMigrationIT
```

### Rolling Back Migrations (Dev Only)

```bash
mvn flyway:clean flyway:migrate
```

**⚠️ WARNING:** This deletes all data. Never run in production.

---

## GraphQL API Development

### Schema-First Approach

1. **Define schema** in `.graphqls` files first:
   - `src/main/resources/graphql/types/` - Domain types
   - `src/main/resources/graphql/inputs/` - Input types
   - `src/main/resources/graphql/schema.graphqls` - Root schema

2. **Implement resolvers** in Java:
   - `@QueryMapping` for queries
   - `@MutationMapping` for mutations
   - `@Argument` for GraphQL arguments

### Example: Adding a New Query

**Step 1: Update schema.graphqls**

```graphql
type Query {
  health: HealthStatus!
  user(id: ID!): User
  events: [Event!]!  # New query
}
```

**Step 2: Create EventResolver.java**

```java
@Controller
public class EventResolver {
    @QueryMapping
    public List<Event> events() {
        // implementation
    }
}
```

**Step 3: Write Integration Test**

```java
@AutoConfigureGraphQlTester
class EventResolverIT extends AbstractIntegrationTest {
    @Test
    void shouldReturnEvents() {
        graphQlTester.document("query { events { id title } }")
            .execute()
            .path("events")
            .entityList(Event.class);
    }
}
```

---

## Code Quality

### Code Formatting (Spotless)

```bash
mvn spotless:apply   # Auto-format code
mvn spotless:check   # Check formatting
```

### Static Analysis (Checkstyle)

```bash
mvn checkstyle:check
```

### Bug Detection (SpotBugs)

```bash
mvn spotbugs:check
```

### Run All Quality Checks

```bash
mvn verify
```

---

## Git Workflow

### Branch Naming

Use the Linear ticket ID in branch names:

```bash
git checkout -b claude/inv-XX-feature-name
```

Examples:
- `claude/inv-18-setup-project-structure`
- `claude/inv-19-implement-user-authentication`

### Before Committing

1. **Update domain README** if you modified a domain
2. **Run tests**: `mvn verify`
3. **Check governance**: Pre-commit hook will remind you

### Creating Commits

Follow conventional commits format:

```bash
git commit -m "feat: add user authentication GraphQL mutation

- Implement createUser mutation
- Add password hashing with BCrypt
- Add integration tests

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

Commit types:
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `test:` - Adding/updating tests
- `refactor:` - Code refactoring
- `chore:` - Maintenance tasks

### Creating Pull Requests

```bash
git push -u origin claude/inv-XX-feature-name
gh pr create --title "Feature: Description" --body "..."
```

Include in PR description:
- Summary of changes
- Test plan
- Any breaking changes
- Link to Linear ticket

---

## Common Issues

### Issue: Port 5432 Already in Use

**Solution:** Stop existing PostgreSQL instance:

```bash
# Stop system PostgreSQL (macOS)
brew services stop postgresql@16

# Or stop other Docker containers
docker ps
docker stop <container-id>
```

### Issue: Tests Fail with "Could not find or load main class"

**Solution:** Clean and rebuild:

```bash
mvn clean install
```

### Issue: Flyway Migration Checksum Mismatch

**Solution:** In development, clean and re-migrate:

```bash
mvn flyway:clean flyway:migrate
```

**⚠️ Never run `flyway:clean` in production!**

### Issue: Testcontainers Docker Connection Error

**Solution:** Ensure Docker is running:

```bash
docker ps  # Should not error
```

On macOS: Restart Docker Desktop

### Issue: OutOfMemoryError During Maven Build

**Solution:** Increase Maven heap size:

```bash
export MAVEN_OPTS="-Xmx2048m"
mvn clean install
```

---

## Code Review Checklist

Before submitting a PR, ensure:

- [ ] All tests pass (`mvn verify`)
- [ ] No Checkstyle violations
- [ ] No SpotBugs warnings
- [ ] Code is formatted (Spotless)
- [ ] Domain README updated (if applicable)
- [ ] GraphQL schema changes documented
- [ ] Database migrations documented
- [ ] Integration tests added for GraphQL changes
- [ ] Commit messages follow conventional commits

---

## Getting Help

- **GitHub Issues:** https://github.com/vasudevamoorthypatnaik/hestia-backend/issues
- **Documentation:** See `.cursor/rules/` for governance documents
- **Project README:** See [CLAUDE.md](CLAUDE.md) for project instructions

---

## Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring GraphQL Documentation](https://docs.spring.io/spring-graphql/reference/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [GraphQL Best Practices](https://graphql.org/learn/best-practices/)
