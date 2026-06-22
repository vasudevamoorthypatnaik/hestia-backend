# Hestia Backend

Backend service for the **Hestia** application — event invitations, RSVPs, and host management.

## Repo context

- **This repo:** backend (API, services, data).
- **Frontend:** [`hestia-frontend`](../hestia-frontend) (sibling path in the Hestia workspace).

## Tech stack

| Layer        | Choice              |
| ------------ | ------------------- |
| Runtime      | Java 21             |
| Framework    | Spring Boot         |
| API          | GraphQL (primary), REST (secondary) |
| Database     | PostgreSQL (AWS RDS)|
| Migrations   | Flyway              |
| Deployment   | Docker, AWS EC2     |
| IaC          | Terraform           |

## Architecture

- **Style:** Modular monolith with domain boundaries (DDD).
- **Principles:** Stateless services, explicit boundaries, production-first, evolvable toward microservices.

The full architecture (domains, API strategy, security, CI/CD, observability, governance) is specified in:

**`.cursor/rules/02_backend_architecutre_stack.mdc`**

## Prerequisites

- **Java 21** - [Download from Adoptium](https://adoptium.net/)
- **Maven 3.8+** - [Installation guide](https://maven.apache.org/install.html)
- **Docker** - [Installation guide](https://docs.docker.com/get-docker/)
- **Docker Compose** - Usually included with Docker Desktop

Verify installation:

```bash
java -version   # Should show Java 21
mvn -version    # Should show Maven 3.8+
docker --version
```

---

## Quickstart (5 Minutes)

### 1. Start PostgreSQL

```bash
cd /Users/vasu/workspaces/hestia/hestia-backend
docker-compose up -d
```

This starts PostgreSQL 16 on `localhost:5432` with database `hestia_dev`.

### 2. Build & Run

```bash
# Run from hestia-app module (multi-module project)
mvn spring-boot:run -pl hestia-app -Dspring-boot.run.profiles=local
```

The application starts on **http://localhost:8080**

**Note:** The `-pl hestia-app` flag is required for multi-module Maven projects.

### 3. Test the API

**Option A: GraphiQL UI** (may have loading issues in some browsers)

Open **GraphiQL** at http://localhost:8080/graphiql and run:

```graphql
query {
  health {
    status
    timestamp
  }
}
```

Expected response:

```json
{
  "data": {
    "health": {
      "status": "OK",
      "timestamp": "2026-02-15T20:30:00.123456Z"
    }
  }
}
```

**Option B: cURL** (recommended if GraphiQL is stuck loading)

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"{ health { status timestamp } }"}'
```

✅ **You're all set!** The backend is running with:
- GraphQL API on `/graphql`
- GraphiQL playground on `/graphiql` (or use cURL/Postman)
- PostgreSQL with Flyway migrations applied
- 3 seed users loaded (dev profile only)

---

## Local Development

### Project Structure (Multi-Module Maven)

```
hestia-backend/
├── hestia-shared/                     # Shared kernel module
│   └── src/main/java/com/hestia/shared/
│       ├── domain/valueobjects/       # Email, Phone, etc.
│       └── infrastructure/graphql/    # GraphQL scalars (DateTime, URL, etc.)
├── hestia-user/                       # User domain module
│   └── src/main/java/com/hestia/user/
│       ├── domain/                    # User entity, value objects
│       ├── application/               # UserService
│       └── infrastructure/            # JPA repos, GraphQL resolvers
├── hestia-event/                      # Event domain module
├── hestia-invitation/                 # Invitation domain module
│   └── src/main/java/com/hestia/invitation/
│       ├── domain/                    # Invitation, Rsvp entities
│       ├── application/               # InvitationService
│       └── infrastructure/            # JPA repos, GraphQL resolvers
├── hestia-app/                        # Main application module
│   ├── src/main/java/                 # HestiaBackendApplication.java
│   └── src/main/resources/
│       ├── graphql/                   # GraphQL schemas (.graphqls)
│       ├── db/migration/              # Flyway migrations (V001-V007)
│       ├── db/seed/                   # Dev seed data (V900)
│       └── application*.yml           # Configuration files
├── pom.xml                            # Parent POM (multi-module aggregator)
├── docker-compose.yml                 # Local PostgreSQL
└── CONTRIBUTING.md                    # Full development guide
```

### Available Commands

| Command | Description |
|---------|-------------|
| `mvn spring-boot:run -pl hestia-app -Dspring-boot.run.profiles=local` | Start application (local profile) |
| `mvn clean install` | Build all modules + run all tests |
| `mvn test` | Run unit tests (all modules) |
| `mvn test -pl hestia-invitation` | Run unit tests (specific module) |
| `mvn -P it verify` | Run integration tests |
| `mvn spotless:apply` | Auto-format code (all modules) |
| `mvn flyway:migrate -pl hestia-app` | Run database migrations |
| `docker-compose up -d` | Start PostgreSQL |
| `docker-compose down` | Stop PostgreSQL |
| `docker-compose logs -f postgres` | View PostgreSQL logs |

### GraphQL API Development

**Schema-first design:** Edit `.graphqls` files first, then implement resolvers.

1. **Edit schema:** `hestia-app/src/main/resources/graphql/schema.graphqls`
2. **Implement resolver:** `hestia-[domain]/src/main/java/com/hestia/[domain]/infrastructure/graphql/YourResolver.java`
3. **Write integration test:** `hestia-[domain]/src/test/java/com/hestia/[domain]/infrastructure/graphql/YourResolverIT.java`
4. **Test with cURL or GraphiQL:** http://localhost:8080/graphiql

#### Available GraphQL Queries (Seed Data)

The dev profile loads 3 sample users automatically:

```bash
# Query Alice Smith
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"{ user(id: \"00000000-0000-0000-0000-000000000001\") { email firstName lastName } }"}'

# Response: {"data":{"user":{"email":"alice.smith@example.com","firstName":"Alice","lastName":"Smith"}}}
```

**Seed User IDs:**
- `00000000-0000-0000-0000-000000000001` - Alice Smith
- `00000000-0000-0000-0000-000000000002` - Bob Johnson
- `00000000-0000-0000-0000-000000000003` - Carol Williams

**All Available Queries:**
- `health` - Health check
- `user(id: ID!)` - Get user by ID
- `invitation(id: ID!)` - Get invitation by ID (requires test data - see below)

**Mutations:**
- `respondToInvitation(input: RespondToInvitationInput!)` - Submit RSVP

#### Loading Test Data (Invitations)

Seed data only includes users. To test invitation queries:

```bash
# Load test data script
docker-compose exec postgres psql -U hestia_dev -d hestia_dev \
  -f /tmp/test-data.sql

# Or copy and execute:
docker cp hestia-app/src/test/resources/db/test-data/invitations-test-data.sql \
  hestia-postgres-dev:/tmp/test-data.sql && \
docker-compose exec postgres psql -U hestia_dev -d hestia_dev -f /tmp/test-data.sql
```

Then query invitation:
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"{ invitation(id: \"123e4567-e89b-12d3-a456-426614174000\") { guestName status event { name } } }"}'
```

### Database Migrations

**Create a new migration:**

```bash
# 1. Create file: src/main/resources/db/migration/V003__add_events_table.sql
# 2. Write idempotent SQL with rollback strategy
# 3. Restart application or run: mvn flyway:migrate
```

**Flyway automatically applies migrations on application startup.**

### Running Tests

```bash
# Unit tests (fast, no database)
mvn test

# Integration tests (Testcontainers + PostgreSQL)
mvn -P it verify

# All tests
mvn verify
```

### Development Workflow

1. **Start PostgreSQL:** `docker-compose up -d`
2. **Start application:** `mvn spring-boot:run -pl hestia-app -Dspring-boot.run.profiles=local`
3. **Make changes** to Java code or GraphQL schemas
4. **Spring DevTools auto-reloads** on code changes (restart may be needed for GraphQL schema changes)
5. **Test with cURL or GraphiQL:**
   ```bash
   # cURL (recommended)
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{"query":"{ health { status timestamp } }"}'

   # Or GraphiQL: http://localhost:8080/graphiql
   ```
6. **Run tests:** `mvn verify`
7. **Format code before commit:** `mvn spotless:apply`

### Profiles

| Profile | Purpose | GraphiQL | Database | Seed Data | Port |
|---------|---------|----------|----------|-----------|------|
| `dev` | Local development | ✅ Enabled | `localhost:5433/hestia_dev` | ✅ 3 users loaded | 5433 |
| `prod` | Production | ❌ Disabled | AWS RDS (env vars) | ❌ Not loaded | N/A |
| `test` | Integration tests | ❌ Disabled | Testcontainers | ❌ Not loaded | Random |

**Note:** PostgreSQL runs on port **5433** (not default 5432) to avoid conflicts with system PostgreSQL.

### Environment Variables (Production Only)

For local development, use `application-dev.yml` (no env vars needed).

For production deployment:

```bash
export DB_URL=jdbc:postgresql://your-rds-endpoint:5432/hestia_prod
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
java -jar hestia-backend.jar --spring.profiles.active=prod
```

### Stopping the Application

- **Press `Ctrl+C`** to stop the Spring Boot application
- **Stop PostgreSQL:** `docker-compose down`

---

## Development Guidelines

- Follow the rules under `.cursor/rules/` for decision-making and architecture
- All DB changes via Flyway migrations (never manual schema edits)
- GraphQL schema-first design (write `.graphqls` files before Java code)
- Hexagonal architecture: Domain layer has NO Spring/JPA dependencies
- Update domain README.md when making domain changes
- Write integration tests for all GraphQL API changes

**Full development guide:** See [CONTRIBUTING.md](CONTRIBUTING.md)

## License

Private / internal. See project governance.
