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

- **Authentication/Authorization**: No seller identity -- any caller can create links and view stats. In production, this would require JWT/OAuth2 authentication tied to Fiverr seller accounts.
- **Rate limiting**: No protection against abuse (e.g., a bot clicking links thousands of times). Would need Redis-based rate limiting per IP/session.
- **URL validation**: Currently accepts any string as a target URL. Should validate that it's a well-formed URL pointing to a fiverr.com domain.
- **Credit ledger service**: Credits are tracked per-click but there's no aggregated seller wallet/balance system. In production, this would be a separate microservice.
- **Distributed short code generation**: Current approach (random + retry) works at low scale but could have collision issues at very high throughput. A distributed ID generator (e.g., Snowflake) would be needed at scale.
- **Click deduplication**: Same user clicking multiple times is counted as multiple clicks. Would need session/IP-based deduplication.
- **Custom short codes**: Sellers cannot choose their own vanity short codes.
- **Link expiration**: Links never expire. Could add TTL support.
- **Monitoring/metrics**: No Prometheus/Grafana integration for production observability.

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
