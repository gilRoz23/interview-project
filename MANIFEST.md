# MANIFEST

## What Works

- **Short link generation**: POST /links creates a unique 7-character alphanumeric short code for any target URL. Uses SecureRandom + Base62 charset with collision retry.
- **Idempotent creation**: Submitting the same target URL returns the existing short link (enforced at both application and database level via unique constraint on `target_url`).
- **Redirect with tracking**: GET /:shortCode returns an immediate 302 redirect to the target URL. Click is recorded asynchronously without blocking the user.
- **Fraud validation simulation**: Each click triggers a simulated fraud check that takes 500ms and returns true/false with 50% probability. Credit ($0.05) is awarded only on valid clicks.
- **Paginated global analytics**: GET /stats returns all links with total clicks, total earnings, and a monthly earnings breakdown. Pagination via `page` and `size` query parameters.
- **Input validation and error handling**: Empty/missing URLs return 400, unknown short codes return 404, with structured JSON error responses.
- **Automated test suite**: 22 tests covering unit tests (service logic, fraud simulation) and integration tests (full HTTP endpoint testing with H2 in-memory DB).
- **Database storage**: PostgreSQL with proper indexing, foreign keys, and unique constraints. Tables auto-created by Hibernate on startup.

## What Is Missing

- **URL validation**: Currently accepts any string as a target URL. Should validate that it's a well-formed URL pointing to a fiverr.com domain.
- **Click deduplication**: Same user clicking multiple times is counted as multiple clicks. Would need session/IP-based deduplication.

## Database Justification: PostgreSQL

PostgreSQL was chosen because:

1. **Aggregation queries**: The /stats endpoint requires GROUP BY month with SUM of earnings -- this is PostgreSQL's strength. A single query handles what would require complex aggregation pipelines in MongoDB or manual counting in Redis.
2. **ACID transactions**: Credit awards must be atomic. A click either records and awards credit, or it doesn't -- no partial state. PostgreSQL guarantees this out of the box.
3. **Unique constraints**: Idempotent link creation is enforced at the database level with a unique constraint on `target_url`. This prevents race conditions that application-level checks alone cannot handle.
4. **Relational model fits naturally**: Links have many click events -- this is a classic one-to-many relationship. Foreign keys enforce referential integrity.
5. **Indexing**: B-tree indexes on `short_code` and `target_url` provide O(log n) lookups for the two most critical queries (redirect and deduplication).

**Why not MongoDB?** The data is relational (links -> clicks) and the primary query pattern is aggregation (stats). MongoDB can do this with aggregation pipelines, but PostgreSQL does it more naturally and efficiently with standard SQL.

**Why not Redis?** Redis is excellent for caching and fast lookups, but lacks the durability and complex querying needed for credit tracking and monthly aggregation. It could complement PostgreSQL as a cache layer in production.

## Trade-offs

| Decision | Choice | Alternative | Reasoning |
|---|---|---|---|
| **Async click processing** | `@Async` (Spring thread pool) | Message queue (RabbitMQ/Kafka) | Simpler for a single-service interview project. In production, a message queue would provide better reliability and decoupling. |
| **Short code strategy** | Random 7-char Base62 | Sequential ID encoding, hash-based | Random codes are unpredictable (good for security), 7 chars gives ~3.5 trillion combinations. Trade-off: requires uniqueness check on each generation. |
| **Monthly aggregation** | Java-side grouping (TreeMap) | Native SQL `DATE_TRUNC` + `GROUP BY` | Java-side grouping is database-agnostic (works with both PostgreSQL and H2 for tests). Trade-off: slightly less efficient for links with millions of clicks, but acceptable at interview scale. |
| **Credit as BigDecimal** | `BigDecimal("0.05")` | `double` or `float` | Never use floating point for money. BigDecimal avoids rounding errors (e.g., 0.1 + 0.2 != 0.3 in floating point). |
| **H2 for tests** | In-memory H2 database | Testcontainers with real PostgreSQL | H2 is faster to start and doesn't require Docker during test runs. Trade-off: minor SQL dialect differences (handled by using JPQL instead of native queries). |
| **No Lombok** | Manual getters/setters | Lombok annotations | Avoids adding a dependency and keeps the code explicit. Trade-off: more boilerplate, but fully transparent. |

## AI Prompts Used (Cursor IDE)

The following prompts were used.

### Prompt 1: Plan

```
I have a Spring Boot 3.5.0 project (Java 21) with PostgreSQL already connected
and running. The project is at ~/fiverr-shortlinks-service.

Here is the project structure:
- Main class: src/main/java/com/interview/interview_project/FiverrShortlinksApplication.java
- DB config: src/main/resources/application.properties (PostgreSQL on localhost:5432/interviewdb, user: admin, password: admin)
- pom.xml already has: spring-boot-starter-web, spring-boot-starter-data-jpa, postgresql driver
- Hibernate ddl-auto is set to "update" (auto-creates tables from @Entity classes)

Here is my assignment:

Fiverr wants to empower sellers to promote their work anywhere - whether it's a LinkedIn post,
a tweet, Instagram bio, client email or a printed QR code. We want sellers to share their work
everywhere their audience is, turning every impression into a potential lead and every click into
Fiverr credits.

However Fiverr URLs can be long, complex, and difficult to share or remember. To make sharing
seamless, Fiverr is introducing Sharable Links - short, clean, trackable URLs that can point to
any seller-owned page on Fiverr (a Gig, Profile, Portfolio item or other promotional destination).

Core loop:
- Generate: A seller generates a short link for their Gig.
- Share: They post it on social media.
- Reward: When someone clicks the link, Fiverr redirects them to the original page and rewards
  the seller with $0.05 in Fiverr credits for each valid click.

We need to design and implement the backend service that powers this experience. Choose the best
DB for this mission.

APIs:
1. POST /links - Short Link Generation: Create an endpoint to accept a target URL and return a
   unique short URL. If multiple requests are made to generate the same URL, the system must
   return the existing URL.
2. GET /:short_code - Redirection and Tracking: Clicking the short link must redirect to the
   target URL. The $0.05 credit is awarded only for links that passed a fraud validation
   (simulate with a function that takes 500ms to complete and returns true or false with 50%
   probability).
3. GET /stats - Global Analytics: Return a paginated list of all generated links. For each link,
   create a grouped report of link performance that aggregates lifetime totals while maintaining
   monthly stats.

Verify endpoints manually with Postman. Handle edge cases (invalid inputs, missing parameters).
Provide an automated test suite that covers core functionality. Include comprehensive README.md
and MANIFEST.md.

Analyze the requirements and create a detailed implementation plan before writing any code.
Choose the best database and justify the decision.
Include: data model, file structure, API design, key implementation details, and testing strategy.
```

### Prompt 2: Clarification Questions

```
Explain the plan to me simply:
- Why PostgreSQL over other databases?
- Why do we need findByTargetUrl() and findByShortCode()?
- What happens when fraud validation returns false?
```

### Prompt 3: Implementation

```
Implement the plan as specified. Create all files:
- JPA entities (Link, ClickEvent) with proper indexes and constraints
- Repositories with custom queries for stats aggregation
- Services: LinkService (idempotent creation, stats), ClickTrackingService (@Async click
  processing), FraudValidationService (500ms, 50% probability)
- DTOs for request/response objects
- GlobalExceptionHandler for 400/404/500 errors
- LinkController with all 3 endpoints: POST /links, GET /:shortCode, GET /stats
- Enable @Async on the main application class
- Add H2 test dependency and test application.properties
- Write unit tests (LinkServiceTest, FraudValidationServiceTest) and integration tests
  (LinkControllerIntegrationTest)
- Write README.md (setup, architecture, testing, AI setup) and MANIFEST.md (what works,
  what's missing, DB justification, trade-offs)

Build, run, and verify all endpoints work. Run all tests and confirm they pass.
```

### Prompt 4: Project Naming

```
Suggest a more descriptive project name that reflects the Fiverr short links assignment.
Then rename the directory, pom.xml artifactId/groupId/name, main class, test class,
and all references. Verify tests still pass after renaming.
```

### Prompt 5: Edge Case Hardening

```
Review the current implementation for edge cases:
- What happens if the database is down when a click comes in?
- Can two concurrent POST /links requests for the same URL create duplicate entries?
- What if the @Async thread pool is exhausted under high load?
Suggest fixes and implement the critical ones.
```

### Prompt 6: API Contract Validation

```
Add input validation to POST /links:
- targetUrl must be a valid URL format (starts with http:// or https://)
- targetUrl must not exceed 2048 characters
- Return clear 400 error messages for each validation failure
Include tests for each validation rule.
```
